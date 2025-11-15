package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSlotDefinition;
import com.jpigeon.ridebattlelib.core.system.event.FormOverrideEvent;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
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
import net.neoforged.neoforge.common.NeoForge;
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
    final Map<ResourceLocation, FormConfig> forms = new HashMap<>();
    private final List<AttributeModifier> baseAttributes = new ArrayList<>();
    private final List<MobEffectInstance> baseEffects = new ArrayList<>();
    private boolean allowDynamicForms = false;

    /**
     * 初始化时需要传入骑士Id
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
     * 设置辅助驱动器物品和装备槽位(可选)
     */
    public RiderConfig setAuxDriverItem(Item item, EquipmentSlot slot) {
        this.auxDriverItem = item;
        this.auxDriverSlot = slot;
        return this;
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
    public RiderConfig addBaseEffect(Holder<MobEffect> effect, int amplifier){
        return addBaseEffect(effect, 114514, amplifier, true);
    }

    /**
     * 设置此骑士是否允许动态形态
     */
    public RiderConfig setAllowDynamicForms(boolean allow) {
        this.allowDynamicForms = allow;
        return this;
    }

    //====================内部方法====================
    // 形态匹配
    public ResourceLocation matchForm(Player player, Map<ResourceLocation, ItemStack> driverItems) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return null;
        FormOverrideEvent overrideEvent = new FormOverrideEvent(player, driverItems, null);
        NeoForge.EVENT_BUS.post(overrideEvent);

        if (overrideEvent.isCanceled()) {
            RideBattleLib.LOGGER.debug("被FormOverrideEvent取消，跳过形态匹配");
            return null;
        }

        ResourceLocation overrideForm = overrideEvent.getOverrideForm();
        if (overrideForm != null) {
            RideBattleLib.LOGGER.debug("形态被覆盖为: {}", overrideForm);
            return overrideForm;
        }

        if (isDriverEmpty(driverItems)) {
            if (baseFormId != null && forms.containsKey(baseFormId) &&
                    forms.get(baseFormId).allowsEmptyDriver()) {
                RideBattleLib.LOGGER.debug("使用允许空驱动器的基础形态: {}", baseFormId);
                return baseFormId;
            } else {
                RideBattleLib.LOGGER.warn("驱动器为空，且没有允许空驱动器的基础形态");
                return null;
            }
        }
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("开始匹配形态，玩家: {}", player.getName().getString());
            RideBattleLib.LOGGER.debug("当前驱动器内容: {}", driverItems);
        }


        // 先检查是否所有“必需槽位”都有有效物品
        for (ResourceLocation slotId : requiredSlots) {
            DriverSlotDefinition slot = getSlotDefinition(slotId);
            if (slot == null) continue;

            ItemStack stack = driverItems.get(slotId);
            if ((stack == null || stack.isEmpty()) && slot.isRequired()) {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("必需槽位 {} 为空", slotId);
                }
                return null; // 必需槽位不能为空
            }
        }

        // 检查辅助必需槽位
        for (ResourceLocation slotId : auxRequiredSlots) {
            DriverSlotDefinition slot = getAuxSlotDefinition(slotId);
            if (slot == null) continue;

            ItemStack stack = driverItems.get(slotId);
            if ((stack == null || stack.isEmpty()) && slot.isRequired()) {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("辅助必需槽位 {} 为空", slotId);
                }
                return null; // 辅助必需槽位不能为空
            }
        }

        // 尝试匹配所有形态
        for (FormConfig formConfig : forms.values()) {
            boolean mainMatches = formConfig.matchesMainSlots(driverItems, config);
            boolean auxMatches = true;

            // 检查形态是否有辅助槽位要求
            boolean formHasAuxRequirements = !formConfig.getAuxRequiredItems().isEmpty();

            if (formHasAuxRequirements) {
                // 形态要求辅助槽位：必须装备辅助驱动器且槽位匹配
                if (hasAuxDriverEquipped(player)) {
                    auxMatches = formConfig.matchesAuxSlots(driverItems, config);
                } else {
                    auxMatches = false; // 未装备辅助驱动器但形态要求→不匹配
                    if (Config.DEBUG_MODE.get()) {
                        RideBattleLib.LOGGER.debug("形态{}需要辅助驱动器，但玩家未装备", formConfig.getFormId());
                    }
                }
            }

            if (mainMatches && auxMatches) {
                ResourceLocation formId = formConfig.getFormId();
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("匹配到的形态ID: {}", formId);
                }
                return formId;
            }
        }

        if (this.allowsDynamicForms()) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("未找到预设形态，尝试创建动态形态");
            }
            try {
                FormConfig dynamicForm = DynamicFormConfig.getOrCreateDynamicForm(player, this, driverItems);
                return dynamicForm.getFormId();
            } catch (Exception e) {
                RideBattleLib.LOGGER.error("动态形态创建失败", e);
            }
        } else {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("该骑士不支持动态形态，跳过动态形态生成");
            }
        }
        RideBattleLib.LOGGER.warn("未找到匹配形态，且没有允许空驱动器的基础形态");
        return null;
    }

    //====================Getter方法====================

    /**
     * 通过玩家变身状态和装备查找激活的驱动器配置
     */
    public static RiderConfig findActiveDriverConfig(Player player) {
        // 方法1：首先检查玩家是否处于变身状态，从变身数据中获取配置
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            HenshinSystem.TransformedData transformedData = HenshinSystem.INSTANCE.getTransformedData(player);
            if (transformedData != null) {
                RiderConfig config = transformedData.config();
                if (config != null) {
                    if (Config.DEBUG_MODE.get()) {
                        RideBattleLib.LOGGER.debug("从变身数据获取驱动器配置: {}", config.getRiderId());
                    }
                    return config;
                }
            }
        }

        // 方法2：如果不在变身状态或变身数据无效，回退到原有的装备检查
        for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
            // 精确匹配驱动器槽位和物品
            ItemStack driverStack = player.getItemBySlot(config.getDriverSlot());
            if (driverStack.is(config.getDriverItem())) {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("从装备槽位获取驱动器配置: {}", config.getRiderId());
                }
                return config;
            }
        }

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("未找到激活的驱动器配置");
        }
        return null;
    }

    /**
     * 快捷获取FormConfig
     */
    public FormConfig getActiveFormConfig(Player player) {
        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.INSTANCE.getDriverItems(player);
        ResourceLocation formId = matchForm(player, driverItems);

        // 优先检查预设形态
        if (forms.containsKey(formId)) {
            return forms.get(formId);
        }

        // 处理动态形态
        return DynamicFormConfig.getDynamicForm(formId);
    }


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
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            HenshinSystem.TransformedData transformedData = HenshinSystem.INSTANCE.getTransformedData(player);
            if (transformedData != null && transformedData.config().getRiderId().equals(this.getRiderId())) {
                // 检查变身时的驱动器快照中是否有辅助槽位物品
                Map<ResourceLocation, ItemStack> driverSnapshot = transformedData.driverSnapshot();
                for (ResourceLocation auxSlotId : getAuxSlotDefinitions().keySet()) {
                    if (driverSnapshot.containsKey(auxSlotId) && !driverSnapshot.get(auxSlotId).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        // 不在变身状态，使用原有的装备检查
        ItemStack auxStack = player.getItemBySlot(auxDriverSlot);
        return !auxStack.isEmpty() && auxStack.is(auxDriverItem);
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

    public List<AttributeModifier> getBaseAttributes() {
        return Collections.unmodifiableList(baseAttributes);
    }

    public List<MobEffectInstance> getBaseEffects() {
        return Collections.unmodifiableList(baseEffects);
    }
}
