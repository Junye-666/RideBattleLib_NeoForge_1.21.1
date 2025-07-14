package com.jpigeon.ridebattlelib.core.system.form;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;

import java.util.*;

public class DynamicEffectRegistry {
    private static final Map<Item, Set<Holder<MobEffect>>> ITEM_EFFECT_MAP = new HashMap<>();

    public static void registerItemEffects(Item item, Holder<MobEffect> effectHolder) {
        ITEM_EFFECT_MAP.computeIfAbsent(item, k -> new HashSet<>())
                .add(effectHolder);
    }

    public static List<Holder<MobEffect>> getEffectsForItem(Item item) {
        return new ArrayList<>(ITEM_EFFECT_MAP.getOrDefault(item, Collections.emptySet()));
    }
}