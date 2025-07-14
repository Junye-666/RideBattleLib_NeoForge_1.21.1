package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DynamicHenshinManager {
    public static void applyDynamicArmor(Player player, DynamicFormConfig formConfig) {
        // 保存原始装备
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        ArmorManager.INSTANCE.saveOriginalGear(player, config);

        // 应用动态盔甲（跳过腿部）
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
