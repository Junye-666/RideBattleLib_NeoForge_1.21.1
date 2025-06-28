package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.UnhenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.*;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinStateSyncPacket;
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

        ResourceLocation formId = config.matchForm(beltItems);
        FormConfig formConfig = RiderRegistry.getForm(formId);

        if (formConfig == null) return;

        // 处理变身逻辑
        if (formConfig.shouldPause()) {
            // 需要暂停的变身流程
            DriverActionManager.INSTANCE.prepareHenshin(player, formId);
        } else if (isTransformed(player)) {
            // 直接切换形态
            switchForm(player, formId);
        } else {
            // 直接变身
            henshin(player, config.getRiderId());
        }
    }

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);
        if (!canHenshin(player) || formId == null) return false;

        // 执行变身
        HenshinHelper.INSTANCE.performHenshin(player, config, formId);

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
        return player.getData(ModAttachments.PLAYER_DATA).getTransformedData() != null;
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
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setHenshinState(state);
        data.setPendingFormId(formId);

        if (player instanceof ServerPlayer serverPlayer) {
            syncHenshinState(serverPlayer);
        }
    }

    //====================网络通信====================

    public static void syncHenshinState(ServerPlayer player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        RideBattleLib.LOGGER.info("同步变身状态: player={}, state={}, pendingForm={}",
                player.getName().getString(), data.getHenshinState(), data.getPendingFormId());

        PacketHandler.sendToClient(player, new HenshinStateSyncPacket(
                player.getUUID(),
                data.getHenshinState(),
                data.getPendingFormId()
        ));
    }

    //====================Getter方法====================
    @Override
    @Nullable
    public TransformedData getTransformedData(Player player) {
        TransformedAttachmentData attachmentData = player.getData(ModAttachments.PLAYER_DATA).getTransformedData();
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

    public static void syncTransformedState(ServerPlayer player) {
        boolean isTransformed = INSTANCE.isTransformed(player);
        PacketHandler.sendToClient(player, new TransformedStatePacket(player.getUUID(), isTransformed));
    }
}
