package com.jpigeon.ridebattlelib.common.config;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.common.api.IHenshinStrategy;
import com.jpigeon.ridebattlelib.common.api.IPenaltyStrategy;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.common.util.RiderUtils;
import com.jpigeon.ridebattlelib.server.event.FindRiderConfigEvent;
import com.jpigeon.ridebattlelib.server.strategy.DefaultHenshinStrategy;
import com.jpigeon.ridebattlelib.server.strategy.DefaultPenaltyStrategy;
import com.jpigeon.ridebattlelib.server.system.DriverSystem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * 假面骑士配置类。
 * <p>
 * 用于定义骑士的驱动器、槽位、形态、触发物品等。
 * <p>
 * 需通过 RiderRegistry.registerRider() 注册。
 */
public class RiderConfig {
    private final ResourceLocation riderId;
    private Item driverItem = Items.AIR;
    private Item auxDriverItem = Items.AIR;
    private EquipmentSlot driverSlot = EquipmentSlot.LEGS;
    private EquipmentSlot auxDriverSlot = EquipmentSlot.OFFHAND;
    private Item triggerItem = Items.AIR;
    private ResourceLocation baseFormId;
    private final Map<ResourceLocation, DriverSlotDefinition> slotDefinitions = new HashMap<>();
    private final Map<ResourceLocation, DriverSlotDefinition> auxSlotDefinitions = new HashMap<>();
    private final Set<ResourceLocation> requiredSlots = new HashSet<>();
    private final Set<ResourceLocation> auxRequiredSlots = new HashSet<>();
    final Map<ResourceLocation, FormConfig> forms = new LinkedHashMap<>();
    private final List<AttributeModifier> baseAttributes = new ArrayList<>();
    private final List<MobEffectInstance> baseEffects = new ArrayList<>();
    private boolean allowDynamicForms = false;
    private IHenshinStrategy henshinStrategy = new DefaultHenshinStrategy();
    private IPenaltyStrategy penaltyStrategy = new DefaultPenaltyStrategy();

    /**
     * 初始化时需要传入骑士Id
     *
     * @param riderId ResourceLocation
     */
    public RiderConfig(ResourceLocation riderId) {
        this.riderId = riderId;
    }

    //====================常用方法====================

    /**
     * 指定驱动器物品
     */
    public RiderConfig setMainDriverItem(Item item, EquipmentSlot slot) {
        this.driverItem = item;
        this.driverSlot = slot;
        return this;
    }

    /**
     * 设置驱动器物品，使用默认槽位（LEGS）
     */
    public RiderConfig setMainDriverItem(Item item) {
        return setMainDriverItem(item, EquipmentSlot.LEGS);
    }

    /**
     * 设置辅助驱动器物品和装备槽位(可选)
     */
    public RiderConfig setAuxDriverItem(Item item, EquipmentSlot slot) {
        this.auxDriverItem = item;
        this.auxDriverSlot = slot;
        return this;
    }

    /**
     * 设置辅助驱动器物品，使用默认槽位（OFFHAND）
     */
    public RiderConfig setAuxDriverItem(Item item) {
        return setAuxDriverItem(item, EquipmentSlot.OFFHAND);
    }

    /**
     * 添加主驱动器槽位
     */
    public RiderConfig addMainDriverSlot(ResourceLocation slotId, List<Item> allowedItems, boolean isRequired, boolean allowReplace) {
        slotDefinitions.put(slotId,
                new DriverSlotDefinition(allowedItems, null, allowReplace, false, isRequired));
        if (isRequired) {
            requiredSlots.add(slotId);
        }
        return this;
    }

    /**
     * 添加辅助驱动器上的槽位
     */
    public RiderConfig addAuxDriverSlot(ResourceLocation slotId, List<Item> allowedItems, boolean isRequired, boolean allowReplace) {
        auxSlotDefinitions.put(slotId, new DriverSlotDefinition(allowedItems, null, allowReplace, true, isRequired));
        if (isRequired) {
            auxRequiredSlots.add(slotId);
        }
        return this;
    }

    /**
     * 指定触发变身用物品（同时需要FormConfig中TriggerType为Item）
     */
    public RiderConfig setTriggerItem(@Nullable Item item) {
        this.triggerItem = item != null ? item : Items.AIR;
        return this;
    }

    /**
     * 为骑士添加形态
     *
     * @param form 你注册的形态Config
     */
    public RiderConfig addForm(FormConfig form) {
        forms.put(form.getFormId(), form);
        if (baseFormId == null) {
            baseFormId = form.getFormId();
        }
        return this;
    }

    /**
     * 设置基础形态
     *
     * @param formId 你注册形态Config中的形态ID
     */
    public void setBaseForm(ResourceLocation formId) {
        if (forms.containsKey(formId)) {
            baseFormId = formId;
        }
    }

    //====================动态适配方法====================

    /**
     * 添加骑士基础属性修饰符（动态形态时的统一修饰符）
     */
    public RiderConfig addBaseAttribute(ResourceLocation attributeId, double amount,
                                        AttributeModifier.Operation operation) {
        baseAttributes.add(new AttributeModifier(attributeId, amount, operation));
        return this;
    }

    /**
     * 添加基础效果（动态形态时的统一效果）
     */
    public RiderConfig addBaseEffect(Holder<MobEffect> effect, int duration,
                                     int amplifier, boolean hideParticles) {
        baseEffects.add(new MobEffectInstance(effect, duration, amplifier, false, !hideParticles));
        return this;
    }

    /**
     * 快速方法
     */
    public RiderConfig addBaseEffect(Holder<MobEffect> effect, int amplifier) {
        return addBaseEffect(effect, 114514, amplifier, true);
    }

    /**
     * 设置此骑士是否允许动态形态
     */
    public RiderConfig setAllowDynamicForms(boolean allow) {
        this.allowDynamicForms = allow;
        return this;
    }

    /**
     * 设置自定义变身逻辑
     */
    public RiderConfig setHenshinStrategy(IHenshinStrategy strategy) {
        this.henshinStrategy = strategy;
        return this;
    }

    /**
     * 设置自定义吃瘪逻辑
     */
    public RiderConfig setPenaltyStrategy(IPenaltyStrategy strategy) {
        this.penaltyStrategy = strategy;
        return this;
    }

    //====================内部方法====================

    /**
     * 通过玩家变身状态和装备查找激活的驱动器配置
     */
    public static RiderConfig findActiveDriverConfig(Player player) {
        if (player == null) return null;

        // 客户端：走缓存
        if (player.level().isClientSide()) {
            ResourceLocation cachedRider = ClientTransformedCache.getRiderId(player.getUUID());
            if (cachedRider != null) {
                return RiderRegistry.getRider(cachedRider);
            }
            // 未变身时，仍需检查装备（右键交互等场景）
            // 但避免每次都遍历：先查缓存里的 driver 物品，再遍历
            for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
                if (config.isEquippedByPlayer(player)) return config;
            }
            return null;
        }

        // 服务端：走遍历
        FindRiderConfigEvent event = new FindRiderConfigEvent(player);
        if (event.isCanceled()) return null;
        if (event.getConfig() != null) {
            RideBattleLib.LOGGER.debug("RiderConfig搜索被外部修改");
            return event.getConfig();
        }

        // 变身数据中获取配置
        if (HenshinUtils.isTransformed(player)) {
            HenshinSessionData data = HenshinUtils.getSessionData(player);
            if (data != null) {
                RiderConfig config = RiderRegistry.getRider(data.riderId());
                if (config != null) return config;
            }
        }

        // 实例方法检查
        for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
            if (config.isEquippedByPlayer(player)) return config;
        }

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("未找到激活的驱动器配置");
        }
        return null;
    }

    /**
     * 检查玩家是否装备了这个骑士的驱动器
     * 子类可以重写此方法以支持不同的装备检查逻辑
     */
    public boolean isEquippedByPlayer(Player player) {
        // 原版装备检查逻辑
        ItemStack driverStack = player.getItemBySlot(this.getDriverSlot());
        return !driverStack.isEmpty() && driverStack.is(this.getDriverItem());
    }

    /**
     * 检查玩家是否装备了这个骑士的辅助驱动器
     * 子类可以重写此方法以支持不同的装备检查逻辑
     */
    public boolean isAuxDriverEquippedByPlayer(Player player) {
        if (this.auxDriverItem == Items.AIR) {
            return false;
        }
        ItemStack auxStack = player.getItemBySlot(this.auxDriverSlot);
        return !auxStack.isEmpty() && auxStack.is(this.auxDriverItem);
    }

    /**
     * 快捷获取FormConfig
     */
    public @Nullable FormConfig getActiveFormConfig(Player player) {
        // 客户端：走缓存
        if (player.level().isClientSide()) {
            ResourceLocation formId = ClientTransformedCache.getCurrentFormId(player.getUUID());
            if (formId == null) return null;
            return RiderRegistry.getForm(player, formId);
        }
        // 服务端：从 session 读（不再每次 matchForm！）
        HenshinSessionData session = HenshinUtils.getSessionData(player);
        if (session != null) {
            return RiderRegistry.getForm(player, session.formId());
        }
        // 未变身：需要匹配
        Map<ResourceLocation, ItemStack> items = DriverSystem.getInstance().getDriverItems(player);
        ResourceLocation formId = FormMatchEngine.match(player, this, items);
        if (formId == null || formId.equals(RiderUtils.NULL)) return null;
        return RiderRegistry.getForm(player, formId);
    }

    //====================Getter方法====================
    //获取骑士Id
    public ResourceLocation getRiderId() {
        return riderId;
    }

    //获取驱动器物品
    public Item getDriverItem() {
        return driverItem;
    }

    public Item getAuxDriverItem() {
        return auxDriverItem;
    }

    //获取驱动器位置
    public EquipmentSlot getDriverSlot() {
        return driverSlot;
    }

    //获取必须物品
    public @Nullable Item getTriggerItem() {
        return triggerItem;
    }

    //获取必要槽位列表
    public Set<ResourceLocation> getRequiredSlots() {
        return Collections.unmodifiableSet(requiredSlots);
    }

    public Set<ResourceLocation> getAuxRequiredSlots() {
        return Collections.unmodifiableSet(auxRequiredSlots);
    }

    //获取槽位定义
    public DriverSlotDefinition getSlotDefinition(ResourceLocation slotId) {
        return slotDefinitions.get(slotId);
    }

    //获取所有槽位定义的不可修改视图
    public Map<ResourceLocation, DriverSlotDefinition> getSlotDefinitions() {
        return Collections.unmodifiableMap(slotDefinitions);
    }

    // 添加形态获取方法
    public FormConfig getForms(ResourceLocation formId) {
        return forms.get(formId);
    }

    public Map<ResourceLocation, FormConfig> getForms() {
        return forms;
    }

    public boolean includesFormConfig(FormConfig formConfig) {
        return this.getForms().containsValue(formConfig);
    }

    public boolean includesFormId(ResourceLocation formId) {
        return this.getForms().containsKey(formId);
    }

    public ResourceLocation getBaseFormId() {
        return baseFormId;
    }

    // 驱动器是否为空
    private boolean isDriverEmpty(Map<ResourceLocation, ItemStack> driverItems) {
        if (driverItems.isEmpty()) return true;
        for (ItemStack stack : driverItems.values()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public boolean hasAuxDriverEquipped(Player player) {
        // 如果在变身状态，从变身数据中检查辅助驱动器
        if (HenshinUtils.isTransformed(player)) {
            HenshinSessionData sessionData = HenshinUtils.getSessionData(player);
            if (sessionData != null && sessionData.riderId().equals(this.getRiderId())) {
                // 检查变身时的驱动器快照中是否有辅助槽位物品
                Map<ResourceLocation, ItemStack> driverSnapshot = sessionData.driverSnapshot();
                for (ResourceLocation auxSlotId : getAuxSlotDefinitions().keySet()) {
                    if (driverSnapshot.containsKey(auxSlotId) && !driverSnapshot.get(auxSlotId).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        // 不在变身状态，使用实例方法检查
        return isAuxDriverEquippedByPlayer(player);
    }

    public DriverSlotDefinition getAuxSlotDefinition(ResourceLocation slotId) {
        return auxSlotDefinitions.get(slotId);
    }

    // 获取所有辅助驱动器槽位
    public Map<ResourceLocation, DriverSlotDefinition> getAuxSlotDefinitions() {
        return Collections.unmodifiableMap(auxSlotDefinitions);
    }

    public boolean allowsDynamicForms() {
        return allowDynamicForms;
    }

    public IHenshinStrategy getHenshinStrategy() {
        return henshinStrategy;
    }

    public IPenaltyStrategy getPenaltyStrategy() {
        return penaltyStrategy;
    }

    public List<AttributeModifier> getBaseAttributes() {
        return Collections.unmodifiableList(baseAttributes);
    }

    public List<MobEffectInstance> getBaseEffects() {
        return Collections.unmodifiableList(baseEffects);
    }
}
