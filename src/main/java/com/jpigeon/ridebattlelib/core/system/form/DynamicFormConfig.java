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

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态形态配置类
 * 整合了所有动态形态相关功能，支持自定义物品-盔甲槽位映射
 */
public class DynamicFormConfig extends FormConfig {
    private final Map<ResourceLocation, ItemStack> driverSnapshot;
    private boolean shouldPause = false;

    private static final Map<Item, Map<EquipmentSlot, Item>> ITEM_ARMOR_MAP = new HashMap<>();
    private static final Map<String, EquipmentSlot> SLOT_PATTERN_ARMOR_MAPPINGS = new HashMap<>();
    private static final Map<ResourceLocation, Map<EquipmentSlot, Item>> UNDERSUIT_REGISTRY = new HashMap<>();
    private static final Map<Item, List<MobEffectInstance>> ITEM_EFFECT_MAP = new HashMap<>();
    private static final Map<Item, List<ItemStack>> ITEM_GRANTED_ITEMS = new HashMap<>();
    private static final Map<ResourceLocation, FormConfig> DYNAMIC_FORMS = new HashMap<>();
    private static final Map<ResourceLocation, Long> LAST_USED = new HashMap<>();
    private static final long UNLOAD_DELAY = 10 * 60 * 1000; // 10分钟未使用则卸载
    private final List<AttributeModifier> dynamicAttributes = new ArrayList<>();
    private final List<MobEffectInstance> dynamicEffects = new ArrayList<>();
    private final List<ItemStack> dynamicGrantedItems = new ArrayList<>();

    static {
        // 设置槽位名称模式到盔甲槽位的映射
        SLOT_PATTERN_ARMOR_MAPPINGS.put("head", EquipmentSlot.HEAD);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("helmet", EquipmentSlot.HEAD);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("hat", EquipmentSlot.HEAD);

        SLOT_PATTERN_ARMOR_MAPPINGS.put("chest", EquipmentSlot.CHEST);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("body", EquipmentSlot.CHEST);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("torso", EquipmentSlot.CHEST);

        SLOT_PATTERN_ARMOR_MAPPINGS.put("legs", EquipmentSlot.LEGS);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("leggings", EquipmentSlot.LEGS);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("pants", EquipmentSlot.LEGS);

        SLOT_PATTERN_ARMOR_MAPPINGS.put("feet", EquipmentSlot.FEET);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("boots", EquipmentSlot.FEET);
        SLOT_PATTERN_ARMOR_MAPPINGS.put("shoes", EquipmentSlot.FEET);
    }

    private static final Map<EquipmentSlot, Item> DEFAULT_UNDERSUIT = new EnumMap<>(EquipmentSlot.class);
    static {
        DEFAULT_UNDERSUIT.put(EquipmentSlot.HEAD, Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.CHEST, Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.LEGS, Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.FEET, Items.AIR);
    }

    public DynamicFormConfig(ResourceLocation formId, Map<ResourceLocation, ItemStack> driverItems, RiderConfig config) {
        super(formId);
        this.driverSnapshot = new HashMap<>(driverItems);
        configureFromItems(config);
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
        String itemId = BuiltInRegistries.ITEM.getKey(armorItem).getPath().toLowerCase();
        if (itemId.contains("helmet") || itemId.contains("head")) return EquipmentSlot.HEAD;
        if (itemId.contains("chestplate") || itemId.contains("chest")) return EquipmentSlot.CHEST;
        if (itemId.contains("leggings") || itemId.contains("legs")) return EquipmentSlot.LEGS;
        if (itemId.contains("boots") || itemId.contains("feet")) return EquipmentSlot.FEET;
        return null;
    }

    /**
     * 注册槽位模式映射
     */
    public static void registerSlotPattern(String pattern, EquipmentSlot armorSlot) {
        SLOT_PATTERN_ARMOR_MAPPINGS.put(pattern.toLowerCase(), armorSlot);
    }

    /**
     * 注册物品效果
     */
    private static void registerItemEffects(Item item, MobEffectInstance... effectInstances) {
        ITEM_EFFECT_MAP.computeIfAbsent(item, k -> new ArrayList<>())
                .addAll(Arrays.asList(effectInstances));
    }

    public static void registerItemEffect(Item item, Holder<MobEffect> effect,
                                          int duration, int amplifier, boolean ambient) {
        registerItemEffects(item, new MobEffectInstance(effect, duration, amplifier, false, ambient));
    }

    public static void registerItemEffect(Item item, Holder<MobEffect> effectHolder) {
        registerItemEffect(item, effectHolder, 114514, 1, false);
    }

    /**
     * 注册物品授予的物品
     */
    public static void registerItemGrantedItems(Item item, ItemStack... grantedItems) {
        ITEM_GRANTED_ITEMS.put(item, Arrays.asList(grantedItems));
    }

    // ==================== 底衣注册方法 ====================

    /**
     * 注册骑士的底衣配置
     */
    public static void registerRiderUndersuit(ResourceLocation riderId,
                                              Item helmet, Item chestplate,
                                              @Nullable Item leggings, Item boots) {
        Map<EquipmentSlot, Item> undersuit = new EnumMap<>(EquipmentSlot.class);
        undersuit.put(EquipmentSlot.HEAD, helmet != null ? helmet : Items.AIR);
        undersuit.put(EquipmentSlot.CHEST, chestplate != null ? chestplate : Items.AIR);
        undersuit.put(EquipmentSlot.LEGS, leggings != null ? leggings : Items.AIR);
        undersuit.put(EquipmentSlot.FEET, boots != null ? boots : Items.AIR);

        UNDERSUIT_REGISTRY.put(riderId, undersuit);
    }

    /**
     * 注册骑士的底衣配置（使用EnumMap）
     */
    public static void registerRiderUndersuit(ResourceLocation riderId, Map<EquipmentSlot, Item> undersuit) {
        UNDERSUIT_REGISTRY.put(riderId, new EnumMap<>(undersuit));
    }

    /**
     * 设置默认底衣配置
     */
    public static void setDefaultUndersuit(Item helmet, Item chestplate, @Nullable Item leggings, Item boots) {
        DEFAULT_UNDERSUIT.put(EquipmentSlot.HEAD, helmet != null ? helmet : Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.CHEST, chestplate != null ? chestplate : Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.LEGS, leggings != null ? leggings : Items.AIR);
        DEFAULT_UNDERSUIT.put(EquipmentSlot.FEET, boots != null ? boots : Items.AIR);
    }

    /**
     * 移除骑士的底衣配置
     */
    public static void removeRiderUndersuit(ResourceLocation riderId) {
        UNDERSUIT_REGISTRY.remove(riderId);
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

    /**
     * 获取骑士的底衣配置
     */
    public static Map<EquipmentSlot, Item> getRiderUndersuit(ResourceLocation riderId) {
        Map<EquipmentSlot, Item> undersuit = UNDERSUIT_REGISTRY.get(riderId);
        if (undersuit != null) {
            return new EnumMap<>(undersuit);
        }
        return new EnumMap<>(DEFAULT_UNDERSUIT);
    }

    /**
     * 检查骑士是否注册了底衣配置
     */
    public static boolean hasRiderUndersuit(ResourceLocation riderId) {
        return UNDERSUIT_REGISTRY.containsKey(riderId);
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


    /**
     * 从驱动器物品配置动态形态
     */
    private void configureFromItems(RiderConfig config) {
        Set<EquipmentSlot> usedSlots = new HashSet<>();

        // 清空动态数据
        dynamicAttributes.clear();
        dynamicEffects.clear();
        dynamicGrantedItems.clear();

        // 应用基础属性和效果到动态存储
        for (AttributeModifier attr : config.getBaseAttributes()) {
            dynamicAttributes.add(new AttributeModifier(attr.id(), attr.amount(), attr.operation()));
        }

        for (MobEffectInstance effect : config.getBaseEffects()) {
            dynamicEffects.add(new MobEffectInstance(effect));
        }

        // 处理槽位物品
        for (Map.Entry<ResourceLocation, ItemStack> entry : driverSnapshot.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            ItemStack stack = entry.getValue();

            if (!stack.isEmpty()) {
                Item item = stack.getItem();

                if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                    RideBattleLib.LOGGER.debug("处理槽位 {} 的物品: {}", slotId, item);
                }

                // 自动确定盔甲槽位
                EquipmentSlot armorSlot = determineArmorSlot(slotId, item);

                if (armorSlot != null) {
                    usedSlots.add(armorSlot);
                    Item armorItem = getArmorForItem(item, armorSlot);

                    if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                        RideBattleLib.LOGGER.debug("槽位 {} 自动映射到盔甲槽位: {}, 盔甲物品: {}",
                                slotId, armorSlot, armorItem);
                    }

                    if (armorItem != Items.AIR) {
                        setArmorForSlot(armorSlot, armorItem);
                        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                            RideBattleLib.LOGGER.debug("已设置盔甲槽位 {} 为 {}", armorSlot, armorItem);
                        }
                    } else {
                        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                            RideBattleLib.LOGGER.debug("未找到物品 {} 在槽位 {} 的盔甲映射", item, armorSlot);
                        }
                    }
                } else {
                    if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                        RideBattleLib.LOGGER.debug("无法为槽位 {} 的物品 {} 确定盔甲槽位", slotId, item);
                    }
                }

                // 添加物品效果到动态存储
                for (MobEffectInstance effect : getEffectsForItem(item)) {
                    dynamicEffects.add(new MobEffectInstance(effect));
                }

                // 添加授予物品到动态存储
                for (ItemStack granted : getGrantedItemsForItem(item)) {
                    dynamicGrantedItems.add(granted.copy());
                }
            }
        }

        // 填充未使用的槽位与底衣
        fillUnusedSlots(config, usedSlots);

        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("动态形态配置完成 - 头盔: {}, 胸甲: {}, 护腿: {}, 靴子: {}",
                    getHelmet(), getChestplate(), getLeggings(), getBoots());
            RideBattleLib.LOGGER.debug("动态属性数量: {}, 动态效果数量: {}, 动态授予物品数量: {}",
                    dynamicAttributes.size(), dynamicEffects.size(), dynamicGrantedItems.size());
        }
    }

    /**
     * 自动确定盔甲槽位
     */
    private EquipmentSlot determineArmorSlot(ResourceLocation slotId, Item item) {
        // 1. 首先检查槽位名称是否包含模式关键词
        String slotPath = slotId.getPath().toLowerCase();
        for (Map.Entry<String, EquipmentSlot> entry : SLOT_PATTERN_ARMOR_MAPPINGS.entrySet()) {
            if (slotPath.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 2. 检查物品是否有特定槽位的盔甲映射
        Map<EquipmentSlot, Item> slotMap = ITEM_ARMOR_MAP.get(item);
        if (slotMap != null && !slotMap.isEmpty()) {
            // 返回第一个定义的槽位
            return slotMap.keySet().iterator().next();
        }

        // 3. 根据物品类型推断
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }

        // 4. 根据物品ID推断
        String itemId = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        if (itemId.contains("helmet") || itemId.contains("head")) return EquipmentSlot.HEAD;
        if (itemId.contains("chestplate") || itemId.contains("chest")) return EquipmentSlot.CHEST;
        if (itemId.contains("leggings") || itemId.contains("legs")) return EquipmentSlot.LEGS;
        if (itemId.contains("boots") || itemId.contains("feet")) return EquipmentSlot.FEET;

        return null;
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
        // 获取该骑士的底衣配置
        Map<EquipmentSlot, Item> undersuit = getUndersuitForRider(config.getRiderId());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !usedSlots.contains(slot)) {
                Item undersuitItem = undersuit.get(slot);
                if (undersuitItem != null && undersuitItem != Items.AIR) {
                    setArmorForSlot(slot, undersuitItem); // 这里填充底衣
                    if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                        RideBattleLib.LOGGER.debug("为未使用槽位 {} 填充底衣: {}", slot, undersuitItem);
                    }
                }
            }
        }
    }

    /**
     * 获取骑士的底衣配置
     */
    private Map<EquipmentSlot, Item> getUndersuitForRider(ResourceLocation riderId) {
        // 优先返回特定骑士的底衣配置
        Map<EquipmentSlot, Item> customUndersuit = UNDERSUIT_REGISTRY.get(riderId);
        if (customUndersuit != null) {
            return customUndersuit;
        }

        // 返回默认底衣
        return new EnumMap<>(DEFAULT_UNDERSUIT);
    }
    
    // ==================== 覆盖方法 ====================

    /**
     * 重写获取属性的方法，返回动态属性
     */
    @Override
    public List<AttributeModifier> getAttributes() {
        // 合并父类属性和动态属性
        List<AttributeModifier> allAttributes = new ArrayList<>(super.getAttributes());
        allAttributes.addAll(dynamicAttributes);
        return Collections.unmodifiableList(allAttributes);
    }

    /**
     * 重写获取效果的方法，返回动态效果
     */
    @Override
    public List<MobEffectInstance> getEffects() {
        // 合并父类效果和动态效果
        List<MobEffectInstance> allEffects = new ArrayList<>(super.getEffects());
        allEffects.addAll(dynamicEffects);
        return Collections.unmodifiableList(allEffects);
    }

    /**
     * 重写获取授予物品的方法，返回动态授予物品
     */
    @Override
    public List<ItemStack> getGrantedItems() {
        // 合并父类授予物品和动态授予物品
        List<ItemStack> allGrantedItems = new ArrayList<>(super.getGrantedItems());
        allGrantedItems.addAll(dynamicGrantedItems);
        return Collections.unmodifiableList(allGrantedItems);
    }

    @Override
    public void setShouldPause(boolean pause) {
        this.shouldPause = pause;
    }

    @Override
    public boolean shouldPause() {
        return shouldPause;
    }
}
