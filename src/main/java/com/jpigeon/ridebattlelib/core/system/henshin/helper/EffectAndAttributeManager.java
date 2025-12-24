package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
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

public class EffectAndAttributeManager {
    public static final EffectAndAttributeManager INSTANCE = new EffectAndAttributeManager();

    public void applyAttributesAndEffects(Player player, ResourceLocation formId) {
        FormConfig form = getFormConfig(player, formId);
        if (form != null) {
            applyAttributesAndEffects(player, form);
        }
    }

    public void removeAttributesAndEffects(Player player, ResourceLocation formId) {
        FormConfig form = getFormConfig(player, formId);
        if (form != null) {
            removeAttributesAndEffects(player, form);
        }
    }

    // 应用属性和效果
    private void applyAttributesAndEffects(Player player, FormConfig form) {
        applyAttributes(player, form);
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
    private void removeAttributesAndEffects(Player player, FormConfig form) {
        removeAttributes(player, form);
        removeEffects(player, form);
        if (form instanceof DynamicFormConfig) {
            for (MobEffectInstance effect : form.getEffects()) {
                // 避免重复添加
                if (!player.hasEffect(effect.getEffect())) {
                    player.removeEffect(effect.getEffect());
                }
            }
        }
    }

    // 效果应用
    private void applyEffects(Player player, FormConfig form) {
        for (MobEffectInstance effect : form.getEffects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
    }

    // 效果移除
    private void removeEffects(Player player, FormConfig form) {
        if (form != null) {
            for (MobEffectInstance effect : form.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    // 属性应用
    private void applyAttributes(Player player, FormConfig form) {
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        // 移除可能存在的旧属性
        for (AttributeModifier modifier : form.getAttributes()) {
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
        for (AttributeModifier modifier : form.getAttributes()) {
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
    private void removeAttributes(Player player, FormConfig form) {
        if (form == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("无法找到形态配置，无法移除属性");
            }
            return;
        }

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("移除形态属性 - 形态: {}, 属性数量: {}",
                    form.getFormId(), form.getAttributes().size());
        }

        // 移除属性修饰符
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;
        for (AttributeModifier modifier : form.getAttributes()) {
            Holder<Attribute> holder = attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).orElse(null);

            if (holder != null) {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifier.id());
                    if (Config.DEBUG_MODE.get()) {
                        RideBattleLib.LOGGER.debug("移除属性修饰符: {} -> {}", modifier.id(), holder.unwrapKey().map(ResourceKey::location).orElse(null));
                    }
                }
            }
        }

        // 记录并报告任何残留效果
        if (Config.DEBUG_MODE.get()) {
            for (Holder<MobEffect> activeEffect : player.getActiveEffectsMap().keySet()) {
                activeEffect.unwrapKey().ifPresent(key ->
                        RideBattleLib.LOGGER.debug("移除残留效果: {}", key.location()));
            }
        }
    }

    // 辅助方法：获取正确的FormConfig
    private FormConfig getFormConfig(Player player, ResourceLocation formId) {
        // 优先从玩家当前骑士获取
        FormConfig form = RiderRegistry.getForm(player, formId);

        // 如果没找到，尝试动态形态
        if (form == null) {
            form = DynamicFormConfig.getDynamicForm(formId);
        }

        if (form == null && Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("未找到形态配置: {} (玩家: {})", formId,
                    player.getName().getString());
        }

        return form;
    }
}
