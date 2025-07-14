package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicArmorRegistry {
    private static final Map<Item, Item> ITEM_ARMOR_MAP = new HashMap<>();

    public static void registerItemArmor(Item sourceItem, Item armorItem) {
        ITEM_ARMOR_MAP.put(sourceItem, armorItem);
    }

    public static Item getArmorForItem(Item item) {
        // 直接从映射表中查找
        return ITEM_ARMOR_MAP.getOrDefault(item, Items.AIR);
    }
}
