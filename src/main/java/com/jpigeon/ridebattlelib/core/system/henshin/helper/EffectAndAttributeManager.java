package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import io.netty.handler.logging.LogLevel;
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

public class EffectAndAttributeManager {
    public static final EffectAndAttributeManager INSTANCE = new EffectAndAttributeManager();
    // 应用属性和效果
    public void applyAttributesAndEffects(Player player, FormConfig form) {
        applyAttributes(player, form );
        applyEffects(player, form);
        // 确保动态形态的效果被应用
        if (form instanceof DynamicFormConfig) {
            for (MobEffectInstance effect : form.getEffects()) {
                // 避免重复添加
                if (!player.hasEffect(effect.getEffect())) {
                    player.addEffect(new MobEffectInstance(effect));
                }
            }
        }
    }

    // 移除属性和效果
    public void removeAttributesAndEffects(Player player, ResourceLocation formId) {
        removeAttributes(player, formId );
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

        // 添加动态形态支持
        if (formConfig == null) {
            formConfig = DynamicFormConfig.getDynamicForm(formId);
        }

        if (formConfig != null) {
            for (MobEffectInstance effect : formConfig.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    // 属性应用
    private void applyAttributes(Player player, FormConfig formConfig) {
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
    private void removeAttributes(Player player, ResourceLocation formId) {

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
        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            for (Holder<MobEffect> activeEffect : player.getActiveEffectsMap().keySet()) {
                activeEffect.unwrapKey().ifPresent(key ->
                        RideBattleLib.LOGGER.debug("移除残留效果: {}", key.location()));
            }
        }
    }
}
