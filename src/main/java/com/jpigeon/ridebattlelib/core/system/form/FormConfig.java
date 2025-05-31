package com.jpigeon.ridebattlelib.core.system.form;


import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class FormConfig {
    private final ResourceLocation formId;
    private Item helmet = Items.AIR;
    private Item chestplate = Items.AIR;
    private @Nullable Item leggings = Items.AIR;
    private Item boots = Items.AIR;
    private final List<AttributeModifier> attributes = new ArrayList<>();
    private final List<MobEffectInstance> effects = new ArrayList<>();
    private final Map<ResourceLocation, Item> requiredItems = new HashMap<>();
    private final List<ResourceLocation> attributeIds = new ArrayList<>(); // 存储属性ID
    public final Map<ResourceLocation, DynamicPart> dynamicParts = new HashMap<>();

    public FormConfig(ResourceLocation formId) {
        this.formId = formId;
    }

    // 盔甲设置方法
    public FormConfig setArmor(Item helmet, Item chestplate, @Nullable Item leggings, Item boots) {
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings != null ? leggings : Items.AIR;
        this.boots = boots;
        return this;
    }

    // 添加属性修饰符
    public FormConfig addAttribute(ResourceLocation attributeId, double amount, AttributeModifier.Operation operation) {
        UUID uuid = UUID.nameUUIDFromBytes(
                (formId + attributeId.toString()).getBytes(StandardCharsets.UTF_8)
        );
        // 使用ResourceLocation作为ID
        attributes.add(new AttributeModifier(attributeId, amount, operation));
        attributeIds.add(attributeId); // 存储属性ID
        return this;
    }

    // 修复 addEffect 方法
    public FormConfig addEffect(Holder<MobEffect> effect, int duration, int amplifier, boolean hideParticles) {
        // 确保每次都创建新的实例
        effects.add(new MobEffectInstance(effect, duration, amplifier, false, hideParticles));
        return this;
    }

    // 添加必需物品
    public FormConfig addRequiredItem(ResourceLocation slotId, Item item) {
        requiredItems.put(slotId, item);
        return this;
    }

    public FormConfig addDynamicPart(ResourceLocation slotId, EquipmentSlot slot,
                                     Map<Item, Item> itemToArmor,
                                     Map<Item, MobEffectInstance> itemToEffect) {
        Objects.requireNonNull(itemToArmor, "itemToArmor不能为null");
        Objects.requireNonNull(itemToEffect, "itemToEffect不能为null");
        // 验证输入参数
        if (slotId == null || slot == null) {
            throw new IllegalArgumentException("动态部件参数不能为空");
        }

        dynamicParts.put(slotId, new DynamicPart(slot, itemToArmor, itemToEffect));
        return this;
    }

    // 匹配验证
    public boolean matches(Map<ResourceLocation, ItemStack> beltItems) {
        for (Map.Entry<ResourceLocation, Item> entry : requiredItems.entrySet()) {
            ItemStack stack = beltItems.get(entry.getKey());
            if (stack == null || stack.isEmpty() || !stack.is(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesDynamic(Map<ResourceLocation, ItemStack> beltItems) {
        // 没有动态部件时不匹配
        if (dynamicParts.isEmpty()) {
            return false;
        }

        for (ResourceLocation slotId : dynamicParts.keySet()) {
            ItemStack stack = beltItems.getOrDefault(slotId, ItemStack.EMPTY);
            if (stack.isEmpty()) return false;

            DynamicPart part = dynamicParts.get(slotId);
            if (part == null || !part.itemToArmor.containsKey(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    // Getter方法
    public ResourceLocation getFormId() {
        return formId;
    }

    public Item getHelmet() {
        return helmet;
    }

    public Item getChestplate() {
        return chestplate;
    }

    public @Nullable Item getLeggings() {
        return leggings;
    }

    public Item getBoots() {
        return boots;
    }

    public List<AttributeModifier> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public List<MobEffectInstance> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public List<ResourceLocation> getAttributeIds() {
        return Collections.unmodifiableList(attributeIds);
    } // 获取属性ID列表

    public Item getDynamicArmor(ResourceLocation slotId, ItemStack stack) {
        DynamicPart part = dynamicParts.get(slotId);
        return part != null ? part.itemToArmor.get(stack.getItem()) : Items.AIR;
    }

    public List<MobEffectInstance> getDynamicEffects(Map<ResourceLocation, ItemStack> beltItems) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (ResourceLocation slotId : dynamicParts.keySet()) {
            ItemStack stack = beltItems.getOrDefault(slotId, ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                DynamicPart part = dynamicParts.get(slotId);
                MobEffectInstance effect = part.itemToEffect.get(stack.getItem());
                if (effect != null) {
                    effects.add(new MobEffectInstance(effect));
                }
            }
        }
        return effects;
    }

    public static class DynamicPart {
        public final EquipmentSlot slot;
        public final Map<Item, Item> itemToArmor;
        public final Map<Item, MobEffectInstance> itemToEffect;

        DynamicPart(EquipmentSlot slot, Map<Item, Item> itemToArmor,
                    Map<Item, MobEffectInstance> itemToEffect) {
            this.slot = slot;
            this.itemToArmor = itemToArmor;
            this.itemToEffect = itemToEffect;
        }
    }
}
