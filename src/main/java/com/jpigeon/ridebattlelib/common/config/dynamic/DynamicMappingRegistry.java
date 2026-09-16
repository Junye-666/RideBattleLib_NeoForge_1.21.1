package com.jpigeon.ridebattlelib.common.config.dynamic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.*;

public final class DynamicMappingRegistry {
    private DynamicMappingRegistry() {
    }

    // ========== 静态表 ==========
    private static final Map<Item, Map<EquipmentSlot, Item>> ITEM_ARMOR = new HashMap<>();
    private static final Map<String, EquipmentSlot> SLOT_PATTERN = new HashMap<>();
    private static final Map<ResourceLocation, Map<EquipmentSlot, Item>> UNDERSUIT = new HashMap<>();
    private static final Map<Item, List<MobEffectInstance>> ITEM_EFFECTS = new HashMap<>();
    private static final Map<Item, List<ItemStack>> ITEM_GRANTS = new HashMap<>();
    private static final Map<EquipmentSlot, Item> DEFAULT_UNDERSUIT = new EnumMap<>(EquipmentSlot.class);

    static {
        SLOT_PATTERN.put("head", EquipmentSlot.HEAD);
        SLOT_PATTERN.put("helmet", EquipmentSlot.HEAD);
        SLOT_PATTERN.put("hat", EquipmentSlot.HEAD);
        SLOT_PATTERN.put("chest", EquipmentSlot.CHEST);
        SLOT_PATTERN.put("body", EquipmentSlot.CHEST);
        SLOT_PATTERN.put("torso", EquipmentSlot.CHEST);
        SLOT_PATTERN.put("legs", EquipmentSlot.LEGS);
        SLOT_PATTERN.put("leggings", EquipmentSlot.LEGS);
        SLOT_PATTERN.put("pants", EquipmentSlot.LEGS);
        SLOT_PATTERN.put("feet", EquipmentSlot.FEET);
        SLOT_PATTERN.put("boots", EquipmentSlot.FEET);
        SLOT_PATTERN.put("shoes", EquipmentSlot.FEET);

        DEFAULT_UNDERSUIT.put(EquipmentSlot.HEAD, Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.CHEST, Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.LEGS, Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.FEET, Items.AIR);
    }

    // ========== 注册：物品 → 盔甲 ==========
    public static void registerItemArmor(Item src, EquipmentSlot slot, Item armor) {
        ITEM_ARMOR.computeIfAbsent(src, k -> new HashMap<>()).put(slot, armor);
    }

    public static void registerItemArmor(Item src, Item armor) {
        EquipmentSlot slot = getAutoArmorSlot(armor);
        if (slot != null) registerItemArmor(src, slot, armor);
    }

    public static void registerSlotPattern(String pattern, EquipmentSlot slot) {
        SLOT_PATTERN.put(pattern.toLowerCase(), slot);
    }

    // ========== 注册：物品 → 效果 ==========
    public static void registerItemEffect(Item item, Holder<MobEffect> effect,
                                          int duration, int amplifier, boolean ambient) {
        ITEM_EFFECTS.computeIfAbsent(item, k -> new ArrayList<>())
                .add(new MobEffectInstance(effect, duration, amplifier, false, ambient));
    }

    public static void registerItemEffect(Item item, Holder<MobEffect> effect) {
        registerItemEffect(item, effect, 114514, 1, false);
    }

    // ========== 注册：物品 → 授予物品 ==========
    public static void registerItemGrantedItems(Item item, ItemStack... grants) {
        ITEM_GRANTS.put(item, Arrays.asList(grants));
    }

    // ========== 注册：底衣 ==========
    public static void registerRiderUndersuit(ResourceLocation riderId,
                                              Item helmet, Item chest,
                                              @Nullable Item leggings, Item boots) {
        Map<EquipmentSlot, Item> map = new EnumMap<>(EquipmentSlot.class);
        map.put(EquipmentSlot.HEAD, helmet != null ? helmet : Items.AIR);
        map.put(EquipmentSlot.CHEST, chest != null ? chest : Items.AIR);
        map.put(EquipmentSlot.LEGS, leggings != null ? leggings : Items.AIR);
        map.put(EquipmentSlot.FEET, boots != null ? boots : Items.AIR);
        UNDERSUIT.put(riderId, map);
    }

    public static void registerRiderUndersuit(ResourceLocation riderId, Map<EquipmentSlot, Item> undersuit) {
        UNDERSUIT.put(riderId, new EnumMap<>(undersuit));
    }

    public static void setDefaultUndersuit(Item helmet, Item chest, @Nullable Item leggings, Item boots) {
        DEFAULT_UNDERSUIT.put(EquipmentSlot.HEAD, helmet != null ? helmet : Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.CHEST, chest != null ? chest : Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.LEGS, leggings != null ? leggings : Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.FEET, boots != null ? boots : Items.AIR);
    }

    public static void removeRiderUndersuit(ResourceLocation riderId) {
        UNDERSUIT.remove(riderId);
    }

    // ========== 查询 ==========
    public static Item getArmorForItem(Item item, EquipmentSlot slot) {
        Map<EquipmentSlot, Item> m = ITEM_ARMOR.get(item);
        return m != null ? m.getOrDefault(slot, Items.AIR) : Items.AIR;
    }

    public static List<MobEffectInstance> getEffectsForItem(Item item) {
        return new ArrayList<>(ITEM_EFFECTS.getOrDefault(item, Collections.emptyList()));
    }

    public static List<ItemStack> getGrantedItemsForItem(Item item) {
        return ITEM_GRANTS.getOrDefault(item, Collections.emptyList());
    }

    public static Map<EquipmentSlot, Item> getRiderUndersuit(ResourceLocation riderId) {
        Map<EquipmentSlot, Item> custom = UNDERSUIT.get(riderId);
        if (custom != null) return new EnumMap<>(custom);
        return new EnumMap<>(DEFAULT_UNDERSUIT);
    }

    public static boolean hasRiderUndersuit(ResourceLocation riderId) {
        return UNDERSUIT.containsKey(riderId);
    }

    /**
     * 供 Generator 查表用（package-private）
     */
    static Map<EquipmentSlot, Item> peekItemArmor(Item item) {
        return ITEM_ARMOR.get(item);
    }

    /**
     * 供 Generator 查表用（package-private）
     */
    static @Nullable EquipmentSlot matchSlotPattern(String slotPath) {
        String lower = slotPath.toLowerCase();
        for (var e : SLOT_PATTERN.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    // ========== 内部工具 ==========
    static @Nullable EquipmentSlot getAutoArmorSlot(Item armor) {
        if (armor instanceof ArmorItem a) return a.getEquipmentSlot();
        String path = BuiltInRegistries.ITEM.getKey(armor).getPath().toLowerCase();
        if (path.contains("helmet") || path.contains("head")) return EquipmentSlot.HEAD;
        if (path.contains("chestplate") || path.contains("chest")) return EquipmentSlot.CHEST;
        if (path.contains("leggings") || path.contains("legs")) return EquipmentSlot.LEGS;
        if (path.contains("boots") || path.contains("feet")) return EquipmentSlot.FEET;
        return null;
    }
}