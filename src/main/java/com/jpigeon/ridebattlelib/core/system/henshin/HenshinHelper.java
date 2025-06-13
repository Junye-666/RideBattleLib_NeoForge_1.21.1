package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinHelper;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.armor.ArmorManager;
import com.jpigeon.ridebattlelib.core.system.henshin.effect.EffectManager;
import com.jpigeon.ridebattlelib.core.system.henshin.item.ItemManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HenshinHelper implements IHenshinHelper {
    public static final HenshinHelper INSTANCE = new HenshinHelper();

    @Override
    public void executeTransform(Player player, RiderConfig config, ResourceLocation formId) {

        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;

        // 添加：触发 INIT 阶段动画事件并检查是否被取消
        AnimationEvent initEvent = new AnimationEvent(player, config.getRiderId(), AnimationPhase.INIT);
        NeoForge.EVENT_BUS.post(initEvent);
        if (initEvent.isCanceled()) {
            RideBattleLib.LOGGER.info("[调试] 变身被中断（事件取消）");
            return; // 如果事件被取消，则停止后续逻辑
        }

        if (config.shouldPause()) {
            // 设置为已暂停状态
            pauseTransformation(player, config.getRiderId());
        } else {
            continueTransformation(player, config.getRiderId());
        }
    }

    private void continueTransformation(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return;

        // 强制重新匹配形态，而不是使用baseForm
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation matchedFormId = config.matchForm(beltItems);
        FormConfig form = RiderRegistry.getForm(matchedFormId);
        if (form == null) return;

        Map<EquipmentSlot, ItemStack> originalGear = ArmorManager.INSTANCE.saveOriginalGear(player, config);

        // 确保只在继续变身时授予物品
        ItemManager.INSTANCE.grantFormItems(player, matchedFormId);

        // 穿戴盔甲
        ArmorManager.INSTANCE.equipArmor(player, form, beltItems);
        // 应用属性和效果
        EffectManager.INSTANCE.applyAttributesAndEffects(player, form, beltItems);
        // 设置为已变身状态
        setTransformed(player, config, form.getFormId(), originalGear, beltItems);

        // player.displayClientMessage(Component.literal("[调试] 形态匹配成功: " + form.getFormId()).withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void executeFormSwitch(Player player, ResourceLocation newFormId) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return;

        ResourceLocation oldFormId = data.formId();

        // 1. 移除旧形态的属性和效果
        EffectManager.INSTANCE.removeEffects(player, oldFormId);
        ItemManager.INSTANCE.removeGrantedItems(player, oldFormId);

        // 2. 应用新形态
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        if (newForm != null) {
            // 3. 装备新盔甲
            ArmorManager.INSTANCE.equipArmor(player, newForm, currentBelt);

            // 4. 应用新属性和效果
            EffectManager.INSTANCE.applyAttributesAndEffects(player, newForm, currentBelt);
            ItemManager.INSTANCE.grantFormItems(player, newFormId);
            // 5. 更新数据
            INSTANCE.setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt);
        }

        // 6. 触发事件
        NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, data.formId(), newFormId));
    }

    @Override
    public void restoreTransformedState(Player player, TransformedAttachmentData attachmentData) {
        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        FormConfig form = RiderRegistry.getForm(attachmentData.formId());

        if (config != null && form != null) {
            // 恢复原始装备
            ArmorManager.INSTANCE.restoreOriginalGear(player, new HenshinSystem.TransformedData(
                    config,
                    attachmentData.formId(),
                    attachmentData.originalGear(),
                    attachmentData.beltSnapshot()
            ));

            // 重新装备盔甲
            ArmorManager.INSTANCE.equipArmor(player, form, attachmentData.beltSnapshot());

            // 重新应用属性
            EffectManager.INSTANCE.applyAttributesAndEffects(player, form, attachmentData.beltSnapshot());

            // 更新变身状态
            HenshinHelper.INSTANCE.setTransformed(player, config, attachmentData.formId(),
                    attachmentData.originalGear(), attachmentData.beltSnapshot());
        }
    }

    public static boolean isPaused(Player player, ResourceLocation riderId) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        return data.isPaused();
    }

    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId,
                               Map<EquipmentSlot, ItemStack> originalGear,
                               Map<ResourceLocation, ItemStack> beltSnapshot) {

        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);

        // 创建新的变身数据
        TransformedAttachmentData transformedData = new TransformedAttachmentData(
                config.getRiderId(),
                formId,
                originalGear,
                beltSnapshot
        );

        // 创建新的持久化数据（使用新的 riderBeltItems 结构）
        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems), // 复制原有的 riderBeltItems
                transformedData
        );

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }

    public void removeTransformed(Player player) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);

        // 创建新的持久化数据（保留 riderBeltItems，清除变身数据）
        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems), // 复制原有的 riderBeltItems
                null // 清除变身数据
        );

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }

    public void performFormSwitch(Player player, ResourceLocation newFormId) {
        // 在切换形态前强制清除所有效果
        EffectManager.INSTANCE.clearAllModEffects(player);

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }

        ResourceLocation oldFormId = data.formId();

        // 即使形态ID相同也强制移除旧效果
        EffectManager.INSTANCE.removeAttributes(player, oldFormId, data.beltSnapshot());

        FormConfig oldForm = RiderRegistry.getForm(oldFormId);

        // === 先移除旧形态的所有效果和属性 ===
        if (oldForm != null) {
            // 使用旧形态的腰带快照移除效果
            EffectManager.INSTANCE.removeAttributes(player, oldFormId, data.beltSnapshot());
        }
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        // 获取当前实时腰带数据
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        // 检查是否需要更新
        boolean needsUpdate = !newFormId.equals(oldFormId);

        if (needsUpdate) {
            // 使用当前腰带数据移除旧效果（关键修改）
            EffectManager.INSTANCE.removeAttributes(player, oldFormId, currentBelt);

            // 应用新效果
            if (newForm != null) {
                ArmorManager.INSTANCE.equipArmor(player, newForm, currentBelt);
                EffectManager.INSTANCE.applyAttributesAndEffects(player, newForm, currentBelt);
            }

            // 更新变身数据（同步当前腰带快照）
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt); // 更新为当前腰带状态

            // 触发事件
            if (!newFormId.equals(oldFormId)) {
                NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, oldFormId, newFormId));
            }
        }
    }

    public static void pauseTransformation(Player player, ResourceLocation riderId) {
        // 存储当前状态到附件中
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setPaused(true);
    }

    public static void resumeTransformation(Player player, ResourceLocation riderId) {
        // 恢复状态并继续执行
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.isPaused()) {
            data.setPaused(false);
            INSTANCE.continueTransformation(player, riderId);
        }
    }
}
