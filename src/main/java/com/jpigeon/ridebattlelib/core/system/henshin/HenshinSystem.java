package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.DriverActivationEvent;
import com.jpigeon.ridebattlelib.core.system.event.UnhenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.*;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinStateSyncPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SyncHenshinStatePacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.TransformedStatePacket;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事从此开始!
 * 假面骑士的变身系统
 */
public class HenshinSystem implements IHenshinSystem {
    public static final HenshinSystem INSTANCE = new HenshinSystem();
    public static final Map<UUID, Boolean> CLIENT_TRANSFORMED_CACHE = new ConcurrentHashMap<>();

    public record TransformedData(
            RiderConfig config,
            ResourceLocation formId,
            Map<EquipmentSlot, ItemStack> originalGear,
            Map<ResourceLocation, ItemStack> beltSnapshot
    ) {
    }

    @Override
    public void driverAction(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(player, beltItems);
        FormConfig formConfig = config.getActiveFormConfig(player);

        if (formConfig == null) return;
        ItemStack driverItem = player.getItemBySlot(config.getDriverSlot());

        DriverActivationEvent driverEvent = new DriverActivationEvent(player, driverItem);
        NeoForge.EVENT_BUS.post(driverEvent);
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setPendingFormId(formId);
        if (data.getHenshinState() != HenshinState.TRANSFORMING) {
            data.setHenshinState(HenshinState.TRANSFORMING);
        }
        // 同步状态
        if (player.level().isClientSide) {
            // 客户端发送同步请求
            PacketHandler.sendToServer(new SyncHenshinStatePacket(
                    HenshinState.TRANSFORMING,
                    formId
            ));
        } else if (player instanceof ServerPlayer serverPlayer) {
            // 服务端直接同步
            HenshinSystem.syncHenshinState(serverPlayer);
        }

        // 处理变身逻辑
        if (formConfig.shouldPause()) {
            // 需要暂停的变身流程
            DriverActionManager.INSTANCE.prepareHenshin(player, formId);
        } else {
            DriverActionManager.INSTANCE.completeTransformation(player);
        }
    }

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);

        // 如果没有装备辅助驱动器，则移除所有辅助槽位
        if (!config.hasAuxDriverEquipped(player)) {
            beltItems = new HashMap<>(beltItems);
            beltItems.keySet().removeAll(config.getAuxSlotDefinitions().keySet());
        }

        ResourceLocation formId = config.matchForm(player, beltItems);
        if (!canHenshin(player) || formId == null) return false;

        // 执行变身
        HenshinHelper.INSTANCE.performHenshin(player, config, formId);

        transitionToState(player, HenshinState.TRANSFORMED, formId);

        if (player instanceof ServerPlayer serverPlayer) {
            syncTransformedState(serverPlayer);
        }

        return true;
    }

    @Override
    public void unHenshin(Player player) {
        TransformedData data = getTransformedData(player);
        if (data != null) {
            UnhenshinEvent.Pre preUnHenshin = new UnhenshinEvent.Pre(player);
            NeoForge.EVENT_BUS.post(preUnHenshin);
            if (preUnHenshin.isCanceled()) return;
            boolean isPenalty = player.getHealth() <= Config.PENALTY_THRESHOLD.get();

            // 清除效果
            EffectAndAttributeManager.INSTANCE.removeAttributesAndEffects(player, data.formId(), data.beltSnapshot());

            // 恢复装备
            ArmorManager.INSTANCE.restoreOriginalGear(player, data);

            // 同步状态
            ArmorManager.INSTANCE.syncEquipment(player);

            // 数据清理
            HenshinHelper.INSTANCE.removeTransformed(player);
            BeltSystem.INSTANCE.returnItems(player);

            if (isPenalty) {
                // 播放特殊解除音效
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                        0.8F, 0.5F);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                syncTransformedState(serverPlayer);
            }

            // 移除给予的物品
            ItemManager.INSTANCE.removeGrantedItems(player, data.formId());
            transitionToState(player, HenshinState.IDLE, null);

            //事件触发
            UnhenshinEvent.Post postUnHenshin = new UnhenshinEvent.Post(player);
            NeoForge.EVENT_BUS.post(postUnHenshin);
        }
    }

    @Override
    public void switchForm(Player player, ResourceLocation newFormId) {
        // 如果新形态ID为null，表示无法匹配形态
        if (newFormId == null) {
            unHenshin(player);
            return;
        }

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        // 确保只在装备了辅助驱动器时才匹配辅助槽位
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        if (!config.hasAuxDriverEquipped(player)) {
            // 过滤掉辅助槽位
            beltItems = new HashMap<>(beltItems);
            beltItems.keySet().removeAll(config.getAuxSlotDefinitions().keySet());
        }

        HenshinHelper.INSTANCE.performFormSwitch(player, newFormId);
        if (player instanceof ServerPlayer serverPlayer) {
            syncTransformedState(serverPlayer);
        }
    }

    //====================检查方法====================

    @Override
    public boolean isTransformed(Player player) {
        // 客户端检查缓存，服务端检查真实数据
        if (player.level().isClientSide) {
            return CLIENT_TRANSFORMED_CACHE.getOrDefault(player.getUUID(), false);
        }
        return player.getData(RiderAttachments.RIDER_DATA).getTransformedData() != null;
    }

    private boolean canHenshin(Player player) {
        if (PenaltySystem.PENALTY_SYSTEM.isInCooldown(player)) {
            if (player instanceof ServerPlayer) {
                player.displayClientMessage(Component.literal("我的身体已经菠萝菠萝哒, 不能再变身了...").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        return true;
    }

    public void transitionToState(Player player, HenshinState state, @Nullable ResourceLocation formId) {
        RiderData oldData = player.getData(RiderAttachments.RIDER_DATA);

        // 创建新数据副本
        RiderData newData = new RiderData(
                new HashMap<>(oldData.mainBeltItems),
                new HashMap<>(oldData.auxBeltItems),
                oldData.getTransformedData(),  // 保留现有变身数据
                state,                         // 新状态
                formId,                        // 新待处理形态
                oldData.getPenaltyCooldownEnd()
        );

        // 保存更新后的数据
        player.setData(RiderAttachments.RIDER_DATA, newData);

        if (player instanceof ServerPlayer serverPlayer) {
            syncHenshinState(serverPlayer);
        }

        // 调试日志
        RideBattleLib.LOGGER.info("状态变更: {} -> {} (形态: {})",
                oldData.getHenshinState(), state, formId);
    }

    //====================网络通信====================

    public static void syncHenshinState(ServerPlayer player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        RideBattleLib.LOGGER.info("同步变身状态: player={}, state={}, pendingForm={}",
                player.getName().getString(), data.getHenshinState(), data.getPendingFormId());

        PacketHandler.sendToClient(player, new HenshinStateSyncPacket(
                player.getUUID(),
                data.getHenshinState(),
                data.getPendingFormId()
        ));
    }

    public static void syncTransformedState(ServerPlayer player) {
        boolean isTransformed = INSTANCE.isTransformed(player);
        PacketHandler.sendToClient(player, new TransformedStatePacket(player.getUUID(), isTransformed));
    }

    //====================Getter方法====================
    @Override
    @Nullable
    public TransformedData getTransformedData(Player player) {
        TransformedAttachmentData attachmentData = player.getData(RiderAttachments.RIDER_DATA).getTransformedData();
        if (attachmentData == null) return null;

        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        if (config == null) return null;

        return new TransformedData(
                config,
                attachmentData.formId(),
                attachmentData.originalGear(),
                attachmentData.beltSnapshot()
        );
    }
}
