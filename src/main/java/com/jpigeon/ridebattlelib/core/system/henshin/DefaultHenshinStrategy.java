package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.HenshinContext;
import com.jpigeon.ridebattlelib.api.IHenshinStrategy;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.data.TransformedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认变身逻辑类
 */
public final class DefaultHenshinStrategy implements IHenshinStrategy {
    public static final DefaultHenshinStrategy INSTANCE = new DefaultHenshinStrategy();

    @Override
    public void performHenshin(Player player, RiderConfig config, ResourceLocation formId) {
        if (config == null || formId == null) return;

        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.getInstance().getDriverItems(player);
        Map<EquipmentSlot, ItemStack> originalGear = HenshinContext.ARMOR.saveOriginalGear(player, config);

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
        HenshinContext.ITEMS.grantFormItems(player, formId);

        // 动态形态特殊处理
        if (formConfig instanceof DynamicFormConfig) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("应用动态形态盔甲");
            }
            DynamicHenshinManager.applyDynamicArmor(player, (DynamicFormConfig) formConfig);
        } else {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("应用普通形态盔甲");
            }
            HenshinContext.ARMOR.equipArmor(player, formConfig);
        }

        // 应用属性和效果
        HenshinContext.EFFECTS.applyAttributesAndEffects(player, formId);

        // 保存变身快照
        saveTransformedSnapshot(player, config, formConfig.getFormId(), originalGear, driverItems);
    }

    @Override
    public void performFormSwitch(Player player, ResourceLocation newFormId) {
        TransformedData data = HenshinSystem.getInstance().getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }
        ResourceLocation oldFormId = data.formId();
        FormConfig newForm = RiderRegistry.getForm(player, newFormId);
        if (newForm == null) {
            newForm = DynamicFormConfig.getDynamicForm(newFormId);
        }

        Map<ResourceLocation, ItemStack> currentDriver = DriverSystem.getInstance().getDriverItems(player);
        boolean needsUpdate = !newFormId.equals(oldFormId);
        if (newForm != null && needsUpdate) {
            if (newForm instanceof DynamicFormConfig dynamicForm) {
                DynamicHenshinManager.applyDynamicArmor(player, dynamicForm); // 应用动态盔甲
            } else {
                HenshinContext.ARMOR.equipArmor(player, newForm); // 普通盔甲
            }
            // 移除旧属性, 效果和物品
            HenshinContext.EFFECTS.removeAttributesAndEffects(player, oldFormId);
            HenshinContext.ITEMS.removeGrantedItems(player, oldFormId);
            // 应用新属性, 效果和物品
            HenshinContext.EFFECTS.applyAttributesAndEffects(player, newFormId);
            HenshinContext.ITEMS.grantFormItems(player, newFormId);
            // 更新数据
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentDriver);
        }
    }

    @Override
    public void unHenshin(Player player, TransformedData data) {
        ResourceLocation formId = data.formId();

        // 先返还物品，再清除效果和装备
        DriverSystem.getInstance().returnItems(player);

        // 清除效果
        HenshinContext.EFFECTS.removeAttributesAndEffects(player, formId);

        // 恢复装备
        HenshinContext.ARMOR.restoreOriginalGear(player, data);

        // 同步状态
        HenshinContext.ARMOR.syncEquipment(player);

        // 数据清理（在返还物品之后）
        removeTransformed(player);

        if (player instanceof ServerPlayer serverPlayer) {
            HenshinContext.DATA_SYNC.syncTransformedState(serverPlayer);
        }

        // 移除给予的物品
        HenshinContext.ITEMS.removeGrantedItems(player, data.formId());

        RideBattleLib.LOGGER.info("玩家 {} 解除变身", player.getName().getString());
    }

    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId, Map<EquipmentSlot, ItemStack> originalGear, Map<ResourceLocation, ItemStack> driverSnapshot) {
        if (config == null) return;
        RiderData oldData = player.getData(RiderAttachments.RIDER_DATA);
        if (Config.DEBUG_MODE.get()) {
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

        if (Config.DEBUG_MODE.get()) {
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
    public void restoreTransformedState(Player player, TransformedData data) {
        if (data == null) return;

        RiderConfig config = data.config();
        FormConfig formConfig = RiderRegistry.getForm(data.formId());
        if (formConfig == null) {
            formConfig = DynamicFormConfig.getDynamicForm(data.formId());
        }

        if (config != null && formConfig != null) {
            // 恢复原始装备
            HenshinContext.ARMOR.restoreOriginalGear(player, data);

            // 重新装备盔甲
            if (formConfig instanceof DynamicFormConfig) {
                DynamicHenshinManager.applyDynamicArmor(player, (DynamicFormConfig) formConfig);
            } else {
                HenshinContext.ARMOR.equipArmor(player, formConfig);
            }

            // 重新应用属性
            HenshinContext.EFFECTS.applyAttributesAndEffects(player, formConfig.getFormId());

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
