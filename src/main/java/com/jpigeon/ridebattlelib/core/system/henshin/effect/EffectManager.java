package com.jpigeon.ridebattlelib.core.system.henshin.effect;

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

public class EffectManager {
    public static final EffectManager INSTANCE = new EffectManager();

    public void applyAttributes(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        // 先移除可能存在的旧属性
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

        // 再应用新属性
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

    public void removeAttributes(Player player, ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems) {
        // 先强制清除所有效果
        clearAllModEffects(player);

        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;

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
                }
            }
        }

        // 记录并报告任何残留效果
        for (Holder<MobEffect> activeEffect : player.getActiveEffectsMap().keySet()) {
            activeEffect.unwrapKey().ifPresent(key -> RideBattleLib.LOGGER.warn("残留效果: {}", key.location()));
        }


    }
    public void clearAllModEffects(Player player) {
        // 清除所有固定效果
        for (FormConfig form : RiderRegistry.getAllForms()) {
            for (MobEffectInstance effect : form.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }

        RideBattleLib.LOGGER.warn("强制清除玩家所有模组效果: {}", player.getName());
    }

    public void removeAttributesAndEffects(Player player, ResourceLocation formId,
                                           Map<ResourceLocation, ItemStack> beltItems) {
        removeAttributes(player, formId, beltItems);
        removeEffects(player, formId);
    }

    // 添加效果移除方法
    public void removeEffects(Player player, ResourceLocation formId) {
        FormConfig form = RiderRegistry.getForm(formId);
        if (form != null) {
            for (MobEffectInstance effect : form.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    // 添加新方法：应用属性和效果
    public void applyAttributesAndEffects(Player player, FormConfig form,
                                          Map<ResourceLocation, ItemStack> beltItems) {
        applyAttributes(player, form, beltItems);
        applyEffects(player, form);
    }

    // 添加效果应用方法
    public void applyEffects(Player player, FormConfig form) {
        for (MobEffectInstance effect : form.getEffects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
    }
}
