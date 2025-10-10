package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DynamicFormConfig extends FormConfig {
    private final Map<ResourceLocation, ItemStack> driverSnapshot;
    private boolean shouldPause = false; // 新增字段

    public DynamicFormConfig(ResourceLocation formId, Map<ResourceLocation, ItemStack> driverItems, RiderConfig config) {
        super(formId);
        this.driverSnapshot = new HashMap<>(driverItems);
        configureFromItems(config);
    }

    private void configureFromItems(RiderConfig config) {
        Set<EquipmentSlot> usedSlots = new HashSet<>();

        // 应用基础属性和效果
        for (AttributeModifier attr : config.getBaseAttributes()) {
            super.addAttribute(attr.id(), attr.amount(), attr.operation());
        }

        for (MobEffectInstance effect : config.getBaseEffects()) {
            super.addEffect(effect.getEffect(), effect.getDuration(),
                    effect.getAmplifier(), !effect.isVisible());
        }

        // 处理槽位物品
        for (Map.Entry<ResourceLocation, ItemStack> entry : driverSnapshot.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            ItemStack stack = entry.getValue();

            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                // 获取配置的盔甲槽位
                boolean isAuxSlot = config.getAuxSlotDefinitions().containsKey(slotId);
                EquipmentSlot armorSlot = config.getArmorSlotFor(slotId, isAuxSlot);

                if (armorSlot != null) {
                    usedSlots.add(armorSlot);
                    setArmorForSlot(armorSlot, DynamicArmorRegistry.getArmorForItem(item));
                }

                // 添加物品效果
                for (MobEffectInstance effect : DynamicEffectRegistry.getEffectsForItem(item)) {
                    addEffect(effect.getEffect(), effect.getDuration(),
                            effect.getAmplifier(), !effect.isVisible());
                }

                // 添加授予物品
                for (ItemStack granted : DynamicGrantedItem.getGrantedItemsForItem(item)) {
                    super.addGrantedItem(granted.copy());
                }
            }
        }

        // 填充未使用的槽位与底衣
        fillUnusedSlots(config, usedSlots);
    }

    private void setArmorForSlot(EquipmentSlot slot, Item armorItem) {
        switch (slot) {
            case HEAD -> setHelmet(armorItem);
            case CHEST -> setChestplate(armorItem);
            case LEGS -> setLeggings(armorItem);
            case FEET -> setBoots(armorItem);
        }
    }

    private void fillUnusedSlots(RiderConfig config, Set<EquipmentSlot> usedSlots) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !usedSlots.contains(slot)) {
                Item commonArmor = config.getCommonArmorMap().get(slot);
                if (commonArmor != null && commonArmor != Items.AIR) {
                    setArmorForSlot(slot, commonArmor);
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