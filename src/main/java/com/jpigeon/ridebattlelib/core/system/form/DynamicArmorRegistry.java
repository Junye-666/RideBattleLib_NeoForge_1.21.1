package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicArmorRegistry {
    private static final Map<Item, Item> ITEM_ARMOR_MAP = new HashMap<>();
    private static final Map<Item, Supplier<Item>> ARMOR_SUPPLIERS = new HashMap<>();

    public static void registerItemArmor(Item sourceItem, Item armorItem) {
        ITEM_ARMOR_MAP.put(sourceItem, armorItem);
    }

    // 添加对自定义盔甲的支持
    public static void registerItemArmorSupplier(Item sourceItem, Supplier<Item> armorSupplier) {
        ARMOR_SUPPLIERS.put(sourceItem, armorSupplier);
    }

    public static Item getArmorForItem(Item item) {
        // 检查供应商
        if (ARMOR_SUPPLIERS.containsKey(item)) {
            return ARMOR_SUPPLIERS.get(item).get();
        }

        // 检查直接映射
        return ITEM_ARMOR_MAP.getOrDefault(item, Items.AIR);
    }
}
