package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private final List<ResourceLocation> attributeIds = new ArrayList<>();
    private final List<ResourceLocation> effectIds = new ArrayList<>();
    private final Map<ResourceLocation, Item> requiredItems = new HashMap<>();
    private final List<ItemStack> grantedItems = new ArrayList<>();
    private boolean allowsEmptyBelt = false;

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

    public void setAllowsEmptyBelt(boolean allow) {
        this.allowsEmptyBelt = allow;
    }

    // 添加属性修饰符
    public FormConfig addAttribute(ResourceLocation attributeId, double amount,
                                   AttributeModifier.Operation operation) {
        UUID uuid = UUID.nameUUIDFromBytes(
                (formId + attributeId.toString()).getBytes(StandardCharsets.UTF_8)
        );
        attributes.add(new AttributeModifier(attributeId, amount, operation));
        attributeIds.add(attributeId);
        return this;
    }

    public FormConfig addEffect(Holder<MobEffect> effect, int duration,
                                int amplifier, boolean hideParticles) {
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        effectIds.add(effectId);
        effects.add(new MobEffectInstance(effect, duration, amplifier, false, hideParticles));
        return this;
    }

    // 添加必需物品
    public FormConfig addRequiredItem(ResourceLocation slotId, Item item) {
        requiredItems.put(slotId, item);
        return this;
    }

    public FormConfig addGrantedItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            grantedItems.add(stack.copy());
        }
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
    }

    public List<ResourceLocation> getEffectIds() {
        return Collections.unmodifiableList(effectIds);
    }

    public boolean allowsEmptyBelt() {
        return allowsEmptyBelt;
    }

    public List<ItemStack> getGrantedItems() {
        return Collections.unmodifiableList(grantedItems);
    }
}
