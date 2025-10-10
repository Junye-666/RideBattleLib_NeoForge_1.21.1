package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;

import java.util.*;

public class DynamicEffectRegistry {
    private static final Map<Item, List<MobEffectInstance>> ITEM_EFFECT_MAP = new HashMap<>();

    // 注册物品效果（支持等级和持续时间）
    public static void registerItemEffects(Item item, MobEffectInstance... effectInstances) {
        ITEM_EFFECT_MAP.computeIfAbsent(item, k -> new ArrayList<>())
                .addAll(Arrays.asList(effectInstances));
    }

    // 注册物品效果
    public static void registerItemEffect(Item item, Holder<MobEffect> effect,
                                          int duration, int amplifier, boolean ambient) {
        registerItemEffects(item, new MobEffectInstance(effect, duration, amplifier, false, !ambient));
    }

    public static List<MobEffectInstance> getEffectsForItem(Item item) {
        return new ArrayList<>(ITEM_EFFECT_MAP.getOrDefault(item, Collections.emptyList()));
    }
}