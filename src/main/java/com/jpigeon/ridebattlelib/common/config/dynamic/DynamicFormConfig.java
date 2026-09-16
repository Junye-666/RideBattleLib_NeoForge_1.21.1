package com.jpigeon.ridebattlelib.common.config.dynamic;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 动态形态配置类
 * 整合了所有动态形态相关功能，支持自定义物品-盔甲槽位映射
 */
public class DynamicFormConfig extends FormConfig {

    private final Map<ResourceLocation, ItemStack> driverSnapshot;
    private boolean shouldPause = false;

    private final List<AttributeModifier> dynamicAttributes = new ArrayList<>();
    private final List<MobEffectInstance> dynamicEffects = new ArrayList<>();
    private final List<ItemStack> dynamicGrantedItems = new ArrayList<>();

    /**
     * 仅由 Generator 调用
     */
    DynamicFormConfig(ResourceLocation formId, Map<ResourceLocation, ItemStack> snapshot) {
        super(formId);
        this.driverSnapshot = new HashMap<>(snapshot);
    }

    // ========== Generator 的填充入口（package-private）==========
    void addDynamicAttribute(AttributeModifier mod) {
        dynamicAttributes.add(mod);
    }

    void addDynamicEffect(MobEffectInstance eff) {
        dynamicEffects.add(eff);
    }

    void addDynamicGranted(ItemStack stack) {
        dynamicGrantedItems.add(stack);
    }

    void applyArmorSlot(EquipmentSlot slot, Item item) {
        switch (slot) {
            case HEAD -> setHelmet(item);
            case CHEST -> setChestplate(item);
            case LEGS -> setLeggings(item);
            case FEET -> setBoots(item);
        }
    }

    // ========== 覆盖聚合 getter ==========
    @Override
    public List<AttributeModifier> getAttributes() {
        List<AttributeModifier> all = new ArrayList<>(super.getAttributes());
        all.addAll(dynamicAttributes);
        return Collections.unmodifiableList(all);
    }

    @Override
    public List<MobEffectInstance> getEffects() {
        List<MobEffectInstance> all = new ArrayList<>(super.getEffects());
        all.addAll(dynamicEffects);
        return Collections.unmodifiableList(all);
    }

    @Override
    public List<ItemStack> getGrantedItems() {
        List<ItemStack> all = new ArrayList<>(super.getGrantedItems());
        all.addAll(dynamicGrantedItems);
        return Collections.unmodifiableList(all);
    }

    @Override
    public DynamicFormConfig setShouldPause(boolean pause) {
        this.shouldPause = pause;
        return this;
    }

    @Override
    public boolean shouldPause() {
        return shouldPause;
    }

    public Map<ResourceLocation, ItemStack> getDriverSnapshot() {
        return Collections.unmodifiableMap(driverSnapshot);
    }
}