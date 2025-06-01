package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.api.IAnimationSystem;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.event.FormDynamicUpdateEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 故事从此开始!
 * 假面骑士的变身系统
 */
public class HenshinSystem implements IHenshinSystem, IAnimationSystem {
    public static final HenshinSystem INSTANCE = new HenshinSystem();

    public record TransformedData(
            RiderConfig config,
            ResourceLocation formId,
            Map<EquipmentSlot, ItemStack> originalGear,
            Map<ResourceLocation, ItemStack> beltSnapshot
    ) {}

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        if (player.level().isClientSide()) return false;
        if (HenshinCore.isOnCooldown(player)) {
            player.displayClientMessage(
                    Component.literal("变身冷却中! 剩余时间: " +
                                    HenshinCore.INSTANCE.getRemainingCooldown(player) + "秒")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        if (PenaltySystem.PENALTY_SYSTEM.isInCooldown(player)) {
            player.displayClientMessage(
                    Component.literal("身体残☆破☆不☆堪，无法变身！").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);

        // 处理空腰带情况
        if (formId == null) {
            if (config.getBaseFormId() != null) {
                FormConfig baseForm = RiderRegistry.getForm(config.getBaseFormId());
                if (baseForm != null && baseForm.allowsEmptyBelt()) {
                    formId = config.getBaseFormId();
                }
            }

            // 如果基础形态也不允许空腰带，则取消变身
            if (formId == null) {
                player.displayClientMessage(
                        Component.literal("无法变身：腰带中没有物品"),
                        true
                );
                return false;
            }
        }

        // 使用核心逻辑执行变身
        HenshinCore.executeTransform(player, config, formId, beltItems);
        return true;
    }

    @Override
    public void unHenshin(Player player) {
        TransformedData data = getTransformedData(player);
        if (data != null) {
            boolean isPenalty = player.getHealth() <= Config.PENALTY_THRESHOLD.get();

            // 1. 清除效果（复用HenshinCore逻辑）
            HenshinCore.INSTANCE.clearAllModEffects(player);

            // 2. 移除属性（保持原有逻辑）
            HenshinCore.INSTANCE.removeAttributes(player, data.formId(), data.beltSnapshot());

            // 3. 恢复装备（保持原有逻辑）
            HenshinCore.INSTANCE.restoreOriginalGear(player, data);

            // 4. 同步状态（保持原有逻辑）
            HenshinCore.INSTANCE.syncEquipment(player);

            // 5. 数据清理（新增HenshinCore集成）
            HenshinCore.startCooldown(player); // 添加变身冷却
            HenshinCore.INSTANCE.removeTransformed(player);
            BeltSystem.INSTANCE.returnItems(player);

            if (isPenalty) {
                // 播放特殊解除音效
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                        0.8F, 0.5F);
            }

            // 6. 事件触发（建议移至HenshinCore）
            onHenshinEnd(player);
        }
    }

    @Override
    public void switchForm(Player player, ResourceLocation newFormId) {
        // 如果新形态ID为null，表示无法匹配形态
        if (newFormId == null) {
            // 根据设计需求选择：
            // 1. 保持当前形态不变
            // 2. 解除变身
            unHenshin(player); // 这里选择解除变身
            return;
        }
        HenshinCore.executeFormSwitch(player, newFormId);
    }

    //====================变身辅助方法====================

    @Override
    public void updateDynamicForm(Player player) {
        TransformedData data = getTransformedData(player);
        if (data == null) return;

        ResourceLocation formId = data.formId();
        FormConfig form = RiderRegistry.getForm(formId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        if (form != null && !form.dynamicParts.isEmpty()) {
            HenshinCore.INSTANCE.equipArmor(player, form, currentBelt);
            HenshinCore.INSTANCE.applyAttributes(player, form, currentBelt);
            HenshinCore.INSTANCE.setTransformed(player, data.config(), formId, data.originalGear(), currentBelt);
            NeoForge.EVENT_BUS.post(new FormDynamicUpdateEvent(player, formId));
        }
    }

    public void restoreTransformedState(Player player, TransformedAttachmentData attachmentData) {
        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        FormConfig form = RiderRegistry.getForm(attachmentData.formId());

        if (config != null && form != null) {
            // 恢复原始装备
            HenshinCore.INSTANCE.restoreOriginalGear(player, new TransformedData(
                    config,
                    attachmentData.formId(),
                    attachmentData.originalGear(),
                    attachmentData.beltSnapshot()
            ));

            // 重新装备盔甲
            HenshinCore.INSTANCE.equipArmor(player, form, attachmentData.beltSnapshot());

            // 重新应用属性
            HenshinCore.INSTANCE.applyAttributes(player, form, attachmentData.beltSnapshot());

            // 更新变身状态
            HenshinCore.INSTANCE.setTransformed(player, config, attachmentData.formId(),
                    attachmentData.originalGear(), attachmentData.beltSnapshot());
        }
    }

    //====================检查方法====================

    @Override
    public boolean isTransformed(Player player) {
        return player.getData(ModAttachments.PLAYER_DATA).transformedData() != null;
    }

    @Override
    public void playHenshinSequence(Player player, ResourceLocation formId, AnimationPhase phase) {
        // 触发动画事件供其他模组扩展
        NeoForge.EVENT_BUS.post(new AnimationEvent(player, formId, phase));
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
}
