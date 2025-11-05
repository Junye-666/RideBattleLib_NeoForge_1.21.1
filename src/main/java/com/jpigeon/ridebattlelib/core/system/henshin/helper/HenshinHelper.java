package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinHelper;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import io.netty.handler.logging.LogLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;


// 变身辅助方法
public final class HenshinHelper implements IHenshinHelper {
    public static final HenshinHelper INSTANCE = new HenshinHelper();
    public static final ArmorManager ARMOR = new ArmorManager();
    public static final EffectAndAttributeManager EFFECT_ATTRIBUTE = new EffectAndAttributeManager();
    public static final ItemManager ITEM = new ItemManager();

    @Override
    public void performHenshin(Player player, RiderConfig config, ResourceLocation formId) {
        if (config == null || formId == null) return;

        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.INSTANCE.getDriverItems(player);
        Map<EquipmentSlot, ItemStack> originalGear = ARMOR.saveOriginalGear(player, config);

        // 获取形态配置（支持动态形态）
        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) {
            formConfig = DynamicFormConfig.getDynamicForm(formId);
        }

        if (formConfig == null) {
            RideBattleLib.LOGGER.warn("尝试变身为未知形态: {}", formId);
            return;
        }

        // 给予形态专属物品
        ITEM.grantFormItems(player, formId);

        // 动态形态特殊处理
        if (formConfig instanceof DynamicFormConfig) {
            if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                RideBattleLib.LOGGER.debug("应用动态形态盔甲");
            }
            DynamicHenshinManager.applyDynamicArmor(player, (DynamicFormConfig) formConfig);
        } else {
            if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                RideBattleLib.LOGGER.debug("应用普通形态盔甲");
            }
            ARMOR.equipArmor(player, formConfig);
        }

        // 应用属性和效果
        EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, formConfig);

        // 保存变身快照
        saveTransformedSnapshot(player, config, formConfig.getFormId(), originalGear, driverItems);

        // 触发后置事件
        HenshinEvent.Post postHenshin = new HenshinEvent.Post(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(postHenshin);
    }

    @Override
    public void performFormSwitch(Player player, ResourceLocation newFormId) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }
        ResourceLocation oldFormId = data.formId();
        if (!newFormId.equals(oldFormId)) {
            FormSwitchEvent.Pre preFormSwitch = new FormSwitchEvent.Pre(player, oldFormId, newFormId);
            NeoForge.EVENT_BUS.post(preFormSwitch);
        }
        FormConfig oldForm = RiderRegistry.getForm(oldFormId);
        if (oldForm == null) {
            DynamicFormConfig.getDynamicForm(oldFormId);
        }

        FormConfig newForm = RiderRegistry.getForm(newFormId);
        if (newForm == null) {
            newForm = DynamicFormConfig.getDynamicForm(newFormId); // 添加动态形态支持
        }
        Map<ResourceLocation, ItemStack> currentDriver = DriverSystem.INSTANCE.getDriverItems(player);
        boolean needsUpdate = !newFormId.equals(oldFormId);
        if (newForm != null && needsUpdate) {
            if (newForm instanceof DynamicFormConfig) {
                DynamicHenshinManager.applyDynamicArmor(player, (DynamicFormConfig) newForm); // 应用动态盔甲
            } else {
                ARMOR.equipArmor(player, newForm); // 普通盔甲
            }
            // 移除旧属性, 效果和物品
            EFFECT_ATTRIBUTE.removeAttributesAndEffects(player, oldFormId );
            ITEM.removeGrantedItems(player, oldFormId);
            // 应用新属性, 效果和物品
            EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, newForm );
            ITEM.grantFormItems(player, newFormId);
            // 更新数据
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentDriver);
        }

        // 触发形态切换事件
        if (!newFormId.equals(oldFormId)) {
            FormSwitchEvent.Post postFormSwitch = new FormSwitchEvent.Post(player, oldFormId, newFormId);
            NeoForge.EVENT_BUS.post(postFormSwitch);
        }
    }

    public void restoreTransformedState(Player player, TransformedAttachmentData attachmentData) {
        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        FormConfig formConfig = RiderRegistry.getForm(attachmentData.formId());

        if (config != null && formConfig != null) {
            // 恢复原始装备
            ARMOR.restoreOriginalGear(player, new HenshinSystem.TransformedData(
                    config,
                    attachmentData.formId(),
                    attachmentData.originalGear(),
                    attachmentData.driverSnapshot()
            ));

            // 重新装备盔甲
            ARMOR.equipArmor(player, formConfig);

            // 重新应用属性
            EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, formConfig );

            // 更新变身状态
            setTransformed(player, config, attachmentData.formId(),
                    attachmentData.originalGear(), attachmentData.driverSnapshot());
        }
    }

    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId, Map<EquipmentSlot, ItemStack> originalGear, Map<ResourceLocation, ItemStack> driverSnapshot) {
        if (config == null) return;
        RiderData oldData = player.getData(RiderAttachments.RIDER_DATA);
        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("保存变身数据 - 骑士: {}, 形态: {}",
                    config.getRiderId(), formId);
        }

        TransformedAttachmentData transformedData = new TransformedAttachmentData(
                config.getRiderId(),
                formId,
                originalGear,
                driverSnapshot
        );

        RiderData newData = new RiderData(
                new HashMap<>(oldData.mainDriverItems),
                new HashMap<>(oldData.auxDriverItems),
                transformedData,
                oldData.getHenshinState(),
                oldData.getPendingFormId(),
                oldData.getPenaltyCooldownEnd(),
                oldData.getCurrentSkillIndex()
        );

        player.setData(RiderAttachments.RIDER_DATA, newData);
    }

    @Override
    public void saveTransformedSnapshot(Player player, RiderConfig config, ResourceLocation formId,
                                        Map<EquipmentSlot, ItemStack> originalGear,
                                        Map<ResourceLocation, ItemStack> driverSnapshot) {
        if (config == null) return;
        RiderData oldData = player.getData(RiderAttachments.RIDER_DATA);

        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("保存变身快照 - 骑士: {}, 形态: {}", config.getRiderId(), formId);
        }

        TransformedAttachmentData transformedData = new TransformedAttachmentData(
                config.getRiderId(),
                formId,
                originalGear,
                driverSnapshot
        );

        RiderData newData = new RiderData(
                new HashMap<>(oldData.mainDriverItems),
                new HashMap<>(oldData.auxDriverItems),
                transformedData,
                oldData.getHenshinState(),
                oldData.getPendingFormId(),
                oldData.getPenaltyCooldownEnd(),
                oldData.getCurrentSkillIndex()
        );

        player.setData(RiderAttachments.RIDER_DATA, newData);
    }

    @Override
    public void restoreTransformedState(Player player, HenshinSystem.TransformedData data) {
        if (data == null) return;

        RiderConfig config = data.config();
        FormConfig formConfig = RiderRegistry.getForm(data.formId());
        if (formConfig == null) {
            formConfig = DynamicFormConfig.getDynamicForm(data.formId());
        }

        if (config != null && formConfig != null) {
            // 恢复原始装备
            ARMOR.restoreOriginalGear(player, data);

            // 重新装备盔甲
            if (formConfig instanceof DynamicFormConfig) {
                DynamicHenshinManager.applyDynamicArmor(player, (DynamicFormConfig) formConfig);
            } else {
                ARMOR.equipArmor(player, formConfig);
            }

            // 重新应用属性
            EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, formConfig);

            // 保存恢复后的状态
            saveTransformedSnapshot(player, config, data.formId(),
                    data.originalGear(), data.driverSnapshot());
        }
    }

    @Override
    public void removeTransformed(Player player) {
        RiderData oldData = player.getData(RiderAttachments.RIDER_DATA);

        RiderData newData = new RiderData(
                new HashMap<>(oldData.mainDriverItems),
                new HashMap<>(oldData.auxDriverItems),
                null,
                oldData.getHenshinState(),
                oldData.getPendingFormId(),
                oldData.getPenaltyCooldownEnd(),
                oldData.getCurrentSkillIndex()
        );

        player.setData(RiderAttachments.RIDER_DATA, newData);
    }
}
