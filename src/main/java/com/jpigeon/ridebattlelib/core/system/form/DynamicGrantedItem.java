package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class DynamicGrantedItem {
    private static final Map<Item, List<ItemStack>> ITEM_GRANTED_ITEMS = new HashMap<>();

    public static void registerItemGrantedItems(Item item, ItemStack... grantedItems) {
        ITEM_GRANTED_ITEMS.put(item, Arrays.asList(grantedItems));
    }

    public static List<ItemStack> getGrantedItemsForItem(Item item) {
        return ITEM_GRANTED_ITEMS.getOrDefault(item, Collections.emptyList());
    }
}
