package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DynamicFormMatcher {
    private final FormConfig form;
    private final Map<Integer, ResourceLocation> cache = new HashMap<>();

    public DynamicFormMatcher(FormConfig form) {
        this.form = form;
    }

    public ResourceLocation matchForm(Map<ResourceLocation, ItemStack> beltItems) {
        int hash = computeHash(beltItems);
        return cache.computeIfAbsent(hash, k -> form.createDynamicFormId(beltItems));
    }

    private int computeHash(Map<ResourceLocation, ItemStack> beltItems) {
        int result = 1;
        for (ResourceLocation slotId : form.dynamicParts.keySet()) {
            ItemStack stack = beltItems.get(slotId);
            ResourceLocation itemId = stack.isEmpty() ?
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "empty") :
                    BuiltInRegistries.ITEM.getKey(stack.getItem());

            // 使用质数乘法减少冲突
            result = 31 * result + slotId.hashCode();
            result = 31 * result + itemId.hashCode();
        }
        return result;
    }
}
