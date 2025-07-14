package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DynamicFormConfig extends FormConfig {
    private final Map<ResourceLocation, ItemStack> beltSnapshot;
    private boolean shouldPause = false; // 新增字段

    public DynamicFormConfig(ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems) {
        super(formId);
        this.beltSnapshot = new HashMap<>(beltItems);
        configureFromItems();
    }

    private void configureFromItems() {
        int slotIndex = 0;
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (Map.Entry<ResourceLocation, ItemStack> entry : beltSnapshot.entrySet()) {
            if (slotIndex >= armorSlots.length) break;

            ItemStack stack = entry.getValue();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                // 设置盔甲映射
                EquipmentSlot slot = armorSlots[slotIndex++];
                switch (slot) {
                    case HEAD -> setHelmet(DynamicArmorRegistry.getArmorForItem(item));
                    case CHEST -> setChestplate(DynamicArmorRegistry.getArmorForItem(item));
                    case LEGS -> setLeggings(DynamicArmorRegistry.getArmorForItem(item));
                    case FEET -> setBoots(DynamicArmorRegistry.getArmorForItem(item));
                }

                // 添加效果 - 使用 Holder
                for (Holder<MobEffect> holder : DynamicEffectRegistry.getEffectsForItem(item)) {
                    // 直接使用 Holder 添加效果
                    addEffect(holder, -1, 0, true);
                }
            }
        }
    }

    @Override
    public void setShouldPause(boolean pause) {
        this.shouldPause = pause;
    }

    @Override
    public boolean shouldPause() {
        return shouldPause;
    }
}