package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class EffectAndAttributeManager {
    public static final EffectAndAttributeManager INSTANCE = new EffectAndAttributeManager();
    // 应用属性和效果
    public void applyAttributesAndEffects(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        applyAttributes(player, form, beltItems);
        applyEffects(player, form);
    }

    // 移除属性和效果
    public void removeAttributesAndEffects(Player player, ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems) {
        removeAttributes(player, formId, beltItems);
        removeEffects(player, formId);
    }

    // 效果应用
    private void applyEffects(Player player, FormConfig form) {
        for (MobEffectInstance effect : form.getEffects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
    }

    // 效果移除
    private void removeEffects(Player player, ResourceLocation formId) {
        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig != null) {
            for (MobEffectInstance effect : formConfig.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    // 属性应用
    private void applyAttributes(Player player, FormConfig formConfig, Map<ResourceLocation, ItemStack> beltItems) {
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        // 移除可能存在的旧属性
        for (AttributeModifier modifier : formConfig.getAttributes()) {
            attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).ifPresent(holder -> {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifier.id()); // 先移除
                }
            });
        }

        // 应用新属性
        for (AttributeModifier modifier : formConfig.getAttributes()) {
            attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).ifPresent(holder -> {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.addTransientModifier(modifier); // 后添加
                }
            });
        }
    }

    // 属性移除
    private void removeAttributes(Player player, ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems) {

        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) return;

        // 移除属性修饰符
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;
        for (AttributeModifier modifier : formConfig.getAttributes()) {
            Holder<Attribute> holder = attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).orElse(null);

            if (holder != null) {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifier.id());
                }
            }
        }
        // 记录并报告任何残留效果
        for (Holder<MobEffect> activeEffect : player.getActiveEffectsMap().keySet()) {
            activeEffect.unwrapKey().ifPresent(key -> RideBattleLib.LOGGER.warn("残留效果: {}", key.location()));
        }
    }
}
