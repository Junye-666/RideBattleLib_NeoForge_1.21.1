package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.api.IAnimationSystem;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
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
    public boolean henshin(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);
        if (!checkPreconditions(player) && formId == null) return false;

        // 使用核心逻辑执行变身
        HenshinHelper.INSTANCE.executeTransform(player, config, formId);

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

            // 1. 清除效果（复用HenshinCore逻辑）
            HenshinHelper.INSTANCE.clearAllModEffects(player);

            // 2. 移除属性（保持原有逻辑）
            HenshinHelper.INSTANCE.removeAttributes(player, data.formId(), data.beltSnapshot());

            // 3. 恢复装备（保持原有逻辑）
            HenshinHelper.INSTANCE.restoreOriginalGear(player, data);

            // 4. 同步状态（保持原有逻辑）
            HenshinHelper.INSTANCE.syncEquipment(player);

            // 5. 数据清理（新增HenshinCore集成）
            HenshinHelper.startCooldown(player); // 添加变身冷却
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
        HenshinHelper.INSTANCE.executeFormSwitch(player, newFormId);
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
        if (HenshinHelper.isOnCooldown(player)) {
            player.displayClientMessage(Component.literal("变身冷却中! 剩余时间: " +
                                    HenshinHelper.INSTANCE.getRemainingCooldown(player) + "秒")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return false;
        }
        return !PenaltySystem.PENALTY_SYSTEM.isInCooldown(player);
    }

    //====================Getter方法====================

    @Nullable
    public RiderConfig getConfig(Player player) {
        TransformedData data = getTransformedData(player);
        return data != null ? data.config() : null;
    }

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
