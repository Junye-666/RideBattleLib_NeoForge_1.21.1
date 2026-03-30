package com.jpigeon.ridebattlelib.server.strategy;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.IHenshinStrategy;
import com.jpigeon.ridebattlelib.common.config.DynamicFormConfig;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.system.DriverSystem;
import com.jpigeon.ridebattlelib.server.system.helper.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
        Map<EquipmentSlot, ItemStack> originalGear = ArmorManager.getInstance().saveOriginalGear(player, config);

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
        ItemManager.getInstance().grantFormItems(player, formId);

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
            ArmorManager.getInstance().equipArmor(player, formConfig);
        }

        // 应用属性和效果
        EffectAndAttributeManager.getInstance().applyAttributesAndEffects(player, formId);

        // 保存变身快照
        HenshinUtils.saveTransformedSnapshot(player, config, formConfig.getFormId(), originalGear, driverItems);

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        if (data.getSessionData() == null) {
            RideBattleLib.LOGGER.error("快照保存失败，sessionData 为 null");
        }
    }

    @Override
    public void performFormSwitch(Player player, HenshinSessionData data, ResourceLocation newFormId) {
        ResourceLocation oldFormId = data.formId();
        FormConfig newForm = RiderRegistry.getForm(player, newFormId);
        if (newForm == null) {
            newForm = DynamicFormConfig.getDynamicForm(newFormId);
        }

        if (newForm == null) return;

        Map<ResourceLocation, ItemStack> currentDriver = DriverSystem.getInstance().getDriverItems(player);
        boolean needsUpdate = !newFormId.equals(oldFormId);

        // 装备新盔甲
        if (needsUpdate) {
            if (newForm instanceof DynamicFormConfig dynamicForm) {
                DynamicHenshinManager.applyDynamicArmor(player, dynamicForm);
            } else {
                ArmorManager.getInstance().equipArmor(player, newForm);
            }

            // 移除旧效果/物品
            EffectAndAttributeManager.getInstance().removeAttributesAndEffects(player, oldFormId);
            ItemManager.getInstance().removeGrantedItems(player, oldFormId);

            // 应用新效果/物品
            EffectAndAttributeManager.getInstance().applyAttributesAndEffects(player, newFormId);
            ItemManager.getInstance().grantFormItems(player, newFormId);

            // 更新数据
            HenshinUtils.saveTransformedSnapshot(player, RiderRegistry.getRider(data.riderId()), newFormId, data.originalGear(), currentDriver);
        }
    }

    @Override
    public void unHenshin(Player player, HenshinSessionData data) {
        ResourceLocation formId = data.formId();

        // 先返还物品，再清除效果和装备
        DriverSystem.getInstance().returnItems(player);

        // 清除效果
        EffectAndAttributeManager.getInstance().removeAttributesAndEffects(player, formId);

        // 恢复装备
        ArmorManager.getInstance().restoreOriginalGear(player, data);

        // 同步状态
        ArmorManager.getInstance().syncEquipment(player);

        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncHenshinState(serverPlayer);
        }

        // 移除给予的物品
        ItemManager.getInstance().removeGrantedItems(player, data.formId());

        RideBattleLib.LOGGER.info("玩家 {} 解除变身", player.getName().getString());
    }
}
