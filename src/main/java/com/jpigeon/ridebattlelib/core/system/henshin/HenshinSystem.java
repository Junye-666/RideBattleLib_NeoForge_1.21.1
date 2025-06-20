package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IAnimationSystem;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.*;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
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
public class HenshinSystem implements IHenshinSystem, IAnimationSystem {
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
        RideBattleLib.LOGGER.debug("进入driverAction");
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);
        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) return;
        if (formConfig.shouldPause()) {
            DriverActionManager.INSTANCE.prepareHenshin(player, formId);
        } else if (HenshinSystem.INSTANCE.isTransformed(player)) {
            ResourceLocation newFormId = config.matchForm(beltItems);
            DriverActionManager.INSTANCE.proceedFormSwitch(player, newFormId);
        } else {
            DriverActionManager.INSTANCE.proceedHenshin(player, config);
        }
    }

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);
        if (!checkPreconditions(player) && formId == null) return false;

        // 使用核心逻辑执行变身
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
            // 6. 事件触发（建议移至HenshinCore）
            // 移除给予的物品
            ItemManager.INSTANCE.removeGrantedItems(player, data.formId());
            onHenshinEnd(player);
        }
    }

    @Override
    public void switchForm(Player player, ResourceLocation newFormId) {
        // 如果新形态ID为null，表示无法匹配形态
        if (newFormId == null) {
            unHenshin(player); // 解除变身
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
        return player.getData(ModAttachments.PLAYER_DATA).transformedData() != null;
    }

    @Override
    public void playHenshinSequence(Player player, ResourceLocation formId, AnimationPhase phase) {
        // 触发动画事件供其他模组扩展
        NeoForge.EVENT_BUS.post(new AnimationEvent(player, formId, phase));
    }

    private boolean checkPreconditions(Player player) {
        if (player.level().isClientSide()) return false;
        if (PenaltySystem.PENALTY_SYSTEM.isInCooldown(player)) {
            player.displayClientMessage(
                    Component.literal("身体残☆破☆不☆堪，无法变身！").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        return !PenaltySystem.PENALTY_SYSTEM.isInCooldown(player);
    }

    //====================Getter方法====================
    @Override
    @Nullable
    public TransformedData getTransformedData(Player player) {
        TransformedAttachmentData attachmentData = player.getData(ModAttachments.PLAYER_DATA).transformedData();
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
