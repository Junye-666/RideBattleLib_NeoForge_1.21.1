package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import io.netty.handler.logging.LogLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态形态配置类 - 重构版
 * 整合了所有动态形态相关功能，支持自定义物品-盔甲槽位映射
 */
public class DynamicFormConfig extends FormConfig {
    private final Map<ResourceLocation, ItemStack> driverSnapshot;
    private boolean shouldPause = false;

    // 动态形态专用注册表
    private static final Map<Item, Map<EquipmentSlot, Item>> ITEM_ARMOR_MAP = new HashMap<>();
    private static final Map<Item, List<MobEffectInstance>> ITEM_EFFECT_MAP = new HashMap<>();
    private static final Map<Item, List<ItemStack>> ITEM_GRANTED_ITEMS = new HashMap<>();
    private static final Map<ResourceLocation, FormConfig> DYNAMIC_FORMS = new HashMap<>();
    private static final Map<ResourceLocation, Long> LAST_USED = new HashMap<>();
    private static final long UNLOAD_DELAY = 10 * 60 * 1000; // 10分钟未使用则卸载

    public DynamicFormConfig(ResourceLocation formId, Map<ResourceLocation, ItemStack> driverItems, RiderConfig config) {
        super(formId);
        this.driverSnapshot = new HashMap<>(driverItems);
        configureFromItems(config);
    }

    /**
     * 从驱动器物品配置动态形态
     */
    private void configureFromItems(RiderConfig config) {
        Set<EquipmentSlot> usedSlots = new HashSet<>();

        // 应用基础属性和效果
        for (AttributeModifier attr : config.getBaseAttributes()) {
            super.addAttribute(attr.id(), attr.amount(), attr.operation());
        }

        for (MobEffectInstance effect : config.getBaseEffects()) {
            super.addEffect(effect.getEffect(), effect.getDuration(),
                    effect.getAmplifier(), !effect.isVisible());
        }

        // 处理槽位物品
        for (Map.Entry<ResourceLocation, ItemStack> entry : driverSnapshot.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            ItemStack stack = entry.getValue();

            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                // 获取配置的盔甲槽位
                boolean isAuxSlot = config.getAuxSlotDefinitions().containsKey(slotId);
                EquipmentSlot armorSlot = config.getArmorSlotFor(slotId, isAuxSlot);

                if (armorSlot != null) {
                    usedSlots.add(armorSlot);
                    Item armorItem = getArmorForItem(item, armorSlot);
                    if (armorItem != Items.AIR) {
                        setArmorForSlot(armorSlot, armorItem);
                    }
                }

                // 添加物品效果
                for (MobEffectInstance effect : getEffectsForItem(item)) {
                    addEffect(effect.getEffect(), effect.getDuration(),
                            effect.getAmplifier(), !effect.isVisible());
                }

                // 添加授予物品
                for (ItemStack granted : getGrantedItemsForItem(item)) {
                    super.addGrantedItem(granted.copy());
                }
            }
        }

        // 填充未使用的槽位与底衣
        fillUnusedSlots(config, usedSlots);
    }

    /**
     * 设置指定槽位的盔甲
     */
    private void setArmorForSlot(EquipmentSlot slot, Item armorItem) {
        switch (slot) {
            case HEAD -> setHelmet(armorItem);
            case CHEST -> setChestplate(armorItem);
            case LEGS -> setLeggings(armorItem);
            case FEET -> setBoots(armorItem);
        }
    }

    /**
     * 填充未使用的盔甲槽位
     */
    private void fillUnusedSlots(RiderConfig config, Set<EquipmentSlot> usedSlots) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !usedSlots.contains(slot)) {
                Item commonArmor = config.getCommonArmorMap().get(slot);
                if (commonArmor != null && commonArmor != Items.AIR) {
                    setArmorForSlot(slot, commonArmor);
                }
            }
        }
    }

    // ==================== 静态注册方法 ====================

    /**
     * 注册物品到盔甲的映射（支持指定槽位）
     * @param sourceItem 源物品
     * @param armorSlot 盔甲槽位
     * @param armorItem 对应的盔甲物品
     */
    public static void registerItemArmor(Item sourceItem, EquipmentSlot armorSlot, Item armorItem) {
        ITEM_ARMOR_MAP.computeIfAbsent(sourceItem, k -> new HashMap<>())
                .put(armorSlot, armorItem);
    }

    /**
     * 注册物品到盔甲的映射（自动分配到合适槽位）
     */
    public static void registerItemArmor(Item sourceItem, Item armorItem) {
        // 根据盔甲类型自动判断槽位
        EquipmentSlot slot = getAutoArmorSlot(armorItem);
        if (slot != null) {
            registerItemArmor(sourceItem, slot, armorItem);
        }
    }

    /**
     * 自动判断盔甲槽位
     */
    private static EquipmentSlot getAutoArmorSlot(Item armorItem) {
        if (armorItem instanceof ArmorItem armor) {
            return armor.getEquipmentSlot();
        }
        // 根据物品ID推断（备选方案）
        String itemId = BuiltInRegistries.ITEM.getKey(armorItem).getPath();
        if (itemId.contains("helmet") || itemId.contains("head")) return EquipmentSlot.HEAD;
        if (itemId.contains("chestplate") || itemId.contains("chest")) return EquipmentSlot.CHEST;
        if (itemId.contains("leggings") || itemId.contains("legs")) return EquipmentSlot.LEGS;
        if (itemId.contains("boots") || itemId.contains("feet")) return EquipmentSlot.FEET;
        return null;
    }

    /**
     * 注册物品效果
     */
    public static void registerItemEffects(Item item, MobEffectInstance... effectInstances) {
        ITEM_EFFECT_MAP.computeIfAbsent(item, k -> new ArrayList<>())
                .addAll(Arrays.asList(effectInstances));
    }

    public static void registerItemEffect(Item item, Holder<MobEffect> effect,
                                          int duration, int amplifier, boolean ambient) {
        registerItemEffects(item, new MobEffectInstance(effect, duration, amplifier, false, !ambient));
    }

    /**
     * 注册物品授予的物品
     */
    public static void registerItemGrantedItems(Item item, ItemStack... grantedItems) {
        ITEM_GRANTED_ITEMS.put(item, Arrays.asList(grantedItems));
    }

    // ==================== 查询方法 ====================

    /**
     * 获取物品在指定槽位对应的盔甲
     */
    public static Item getArmorForItem(Item item, EquipmentSlot armorSlot) {
        Map<EquipmentSlot, Item> slotMap = ITEM_ARMOR_MAP.get(item);
        return slotMap != null ? slotMap.getOrDefault(armorSlot, Items.AIR) : Items.AIR;
    }

    public static List<MobEffectInstance> getEffectsForItem(Item item) {
        return new ArrayList<>(ITEM_EFFECT_MAP.getOrDefault(item, Collections.emptyList()));
    }

    public static List<ItemStack> getGrantedItemsForItem(Item item) {
        return ITEM_GRANTED_ITEMS.getOrDefault(item, Collections.emptyList());
    }

    // ==================== 动态形态管理 ====================

    /**
     * 获取或创建动态形态
     */
    public static FormConfig getOrCreateDynamicForm(Player player, RiderConfig config,
                                                    Map<ResourceLocation, ItemStack> driverItems) {
        ResourceLocation formId = generateFormId(config.getRiderId(), driverItems);

        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("创建动态形态: {}", formId);
            RideBattleLib.LOGGER.debug("槽位内容: {}", driverItems.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue().getItem())
                    .collect(Collectors.joining(", ")));
        }

        // 检查缓存
        if (DYNAMIC_FORMS.containsKey(formId)) {
            LAST_USED.put(formId, System.currentTimeMillis());
            return DYNAMIC_FORMS.get(formId);
        }

        // 创建新形态
        FormConfig form = new DynamicFormConfig(formId, driverItems, config);
        FormConfig baseForm = config.getForms(config.getBaseFormId());

        if (baseForm != null) {
            form.setTriggerType(baseForm.getTriggerType());
            form.setShouldPause(baseForm.shouldPause());
        } else {
            form.setTriggerType(TriggerType.KEY);
        }

        DYNAMIC_FORMS.put(formId, form);
        LAST_USED.put(formId, System.currentTimeMillis());

        // 自动清理任务
        MinecraftServer server = player.getServer();
        if (server != null && server.getTickCount() % 6000 == 0) {
            server.execute(DynamicFormConfig::cleanupUnusedForms);
        }

        return form;
    }

    /**
     * 生成动态形态ID
     */
    private static ResourceLocation generateFormId(ResourceLocation riderId,
                                                   Map<ResourceLocation, ItemStack> driverItems) {
        String baseId = riderId.getPath().replace("kamen_rider_", "");
        Set<String> itemParts = new LinkedHashSet<>();

        for (ItemStack stack : driverItems.values()) {
            if (!stack.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String trimmedId = trimCommonSuffix(itemId.getPath());
                itemParts.add(trimmedId);
            }
        }

        String formPath = baseId + "_" + String.join("_", itemParts);
        return ResourceLocation.fromNamespaceAndPath(riderId.getNamespace(), formPath);
    }

    private static String trimCommonSuffix(String itemId) {
        String[] suffixes = {"_memory", "_medal", "_switch", "_lock", "_gashat",
                "_card", "_full_bottle", "_watch", "_fantasy_book", "_buckle"};
        for (String suffix : suffixes) {
            if (itemId.endsWith(suffix)) {
                return itemId.substring(0, itemId.length() - suffix.length());
            }
        }
        return itemId;
    }

    /**
     * 清理未使用的动态形态
     */
    public static void cleanupUnusedForms() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ResourceLocation, FormConfig>> it = DYNAMIC_FORMS.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<ResourceLocation, FormConfig> entry = it.next();
            long lastUsed = LAST_USED.getOrDefault(entry.getKey(), 0L);

            if (now - lastUsed > UNLOAD_DELAY) {
                if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                    RideBattleLib.LOGGER.debug("卸载动态形态: {}", entry.getKey());
                }
                it.remove();
                LAST_USED.remove(entry.getKey());
            }
        }
    }

    /**
     * 获取动态形态
     */
    public static FormConfig getDynamicForm(ResourceLocation formId) {
        return DYNAMIC_FORMS.get(formId);
    }

    // ==================== 覆盖方法 ====================

    @Override
    public void setShouldPause(boolean pause) {
        this.shouldPause = pause;
    }

    @Override
    public boolean shouldPause() {
        return shouldPause;
    }
}
