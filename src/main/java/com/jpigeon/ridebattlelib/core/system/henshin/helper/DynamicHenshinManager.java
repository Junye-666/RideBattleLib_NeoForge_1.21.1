package com.jpigeon.ridebattlelib.core.system.henshin.helper;


import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 动态变身管理器 - 简化版
 * 处理动态形态的特殊变身逻辑
 */
public class DynamicHenshinManager {

    /**
     * 应用动态盔甲（跳过腿部）
     */
    public static void applyDynamicArmor(Player player, DynamicFormConfig formConfig) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        // 应用动态盔甲
        if (formConfig.getHelmet() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(formConfig.getHelmet()));
        }
        if (formConfig.getChestplate() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(formConfig.getChestplate()));
        }
        if (formConfig.getBoots() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(formConfig.getBoots()));
        }

        // 确保盔甲立即生效
        ArmorManager.INSTANCE.syncEquipment(player);
    }
}
