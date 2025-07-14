package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DynamicFormConfig extends FormConfig {
    private final Map<ResourceLocation, ItemStack> beltSnapshot;
    private boolean shouldPause = false; // 新增字段

    public DynamicFormConfig(ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems, RiderConfig config) {
        super(formId);
        this.beltSnapshot = new HashMap<>(beltItems);
        configureFromItems(config);
    }

    private void configureFromItems(RiderConfig config) {
        int slotIndex = 0;
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };
        Set<EquipmentSlot> usedSlots = new HashSet<>();

        for (Map.Entry<ResourceLocation, ItemStack> entry : beltSnapshot.entrySet()) {
            if (slotIndex >= armorSlots.length) break;

            ItemStack stack = entry.getValue();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                EquipmentSlot slot = armorSlots[slotIndex++];
                usedSlots.add(slot);

                // 设置盔甲映射
                switch (slot) {
                    case HEAD -> setHelmet(DynamicArmorRegistry.getArmorForItem(item));
                    case CHEST -> setChestplate(DynamicArmorRegistry.getArmorForItem(item));
                    case LEGS -> setLeggings(DynamicArmorRegistry.getArmorForItem(item));
                    case FEET -> setBoots(DynamicArmorRegistry.getArmorForItem(item));
                }

                for (Holder<MobEffect> holder : DynamicEffectRegistry.getEffectsForItem(item)) {
                    addEffect(holder, -1, 0, true);
                }
            }
        }

        for (EquipmentSlot slot : armorSlots) {
            if (!usedSlots.contains(slot)) {
                Item commonArmor = config.getCommonArmorMap().get(slot);
                if (commonArmor != null && commonArmor != Items.AIR) {
                    switch (slot) {
                        case HEAD -> setHelmet(commonArmor);
                        case CHEST -> setChestplate(commonArmor);
                        case LEGS -> setLeggings(commonArmor);
                        case FEET -> setBoots(commonArmor);
                    }
                    RideBattleLib.LOGGER.debug("为槽位 {} 设置底衣: {}", slot, commonArmor);
                }
            }
        }

        for (Map.Entry<ResourceLocation, ItemStack> entry : beltSnapshot.entrySet()) {
            ItemStack stack = entry.getValue();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                for (ItemStack granted : DynamicGrantedItem.getGrantedItemsForItem(item)) {
                    super.addGrantedItem(granted.copy());
                    RideBattleLib.LOGGER.debug("添加GrantedItem: {} -> {}", item, granted);
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