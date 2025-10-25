package com.jpigeon.ridebattlelib.core.system.henshin.helper;


import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import io.netty.handler.logging.LogLevel;
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
     * 应用动态盔甲
     */
    public static void applyDynamicArmor(Player player, DynamicFormConfig formConfig) {
        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("应用动态形态盔甲 - 头盔: {}, 胸甲: {}, 护腿: {}, 靴子: {}",
                    formConfig.getHelmet(), formConfig.getChestplate(), formConfig.getLeggings(), formConfig.getBoots());
        }

        // 应用动态盔甲
        if (formConfig.getHelmet() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(formConfig.getHelmet()));
        }
        if (formConfig.getChestplate() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(formConfig.getChestplate()));
        }
        if (formConfig.getLeggings() != Items.AIR) {
            if (formConfig.getLeggings() != null) {
                player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(formConfig.getLeggings()));
            }
        }
        if (formConfig.getBoots() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(formConfig.getBoots()));
        }

        // 确保盔甲立即生效
        ArmorManager.INSTANCE.syncEquipment(player);
    }
}