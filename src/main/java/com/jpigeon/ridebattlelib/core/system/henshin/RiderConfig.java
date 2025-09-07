package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.belt.SlotDefinition;
import com.jpigeon.ridebattlelib.core.system.event.FormOverrideEvent;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormManager;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * 实现假面骑士实例相关的注册
 * 骑士ID
 * 所需驱动器
 * 必要物品
 * 必要条件
 * 关联盔甲
 * 等等
 */
public class RiderConfig {
    private final ResourceLocation riderId;
    private Item driverItem = Items.AIR;
    private Item auxDriverItem = Items.AIR;
    private EquipmentSlot driverSlot = EquipmentSlot.LEGS;
    private EquipmentSlot auxDriverSlot = EquipmentSlot.OFFHAND;
    private Item triggerItem = Items.AIR;
    private ResourceLocation baseFormId;
    private final Map<ResourceLocation, SlotDefinition> slotDefinitions = new HashMap<>();
    private final Map<ResourceLocation, SlotDefinition> auxSlotDefinitions = new HashMap<>();
    private final Set<ResourceLocation> requiredSlots = new HashSet<>();
    private final Set<ResourceLocation> auxRequiredSlots = new HashSet<>();
    final Map<ResourceLocation, FormConfig> forms = new HashMap<>();
    private final Map<EquipmentSlot, Item> commonArmorMap = new EnumMap<>(EquipmentSlot.class);
    private boolean allowDynamicForms = false;

    //====================初始化方法====================

    //骑士Id初始化
    public RiderConfig(ResourceLocation riderId) {
        this.riderId = riderId;
    }

    //====================Setter方法====================

    //指定驱动器物品
    public RiderConfig setDriverItem(Item item, EquipmentSlot slot) {
        this.driverItem = item;
        this.driverSlot = slot;
        return this;
    }

    //指定触发用物品
    public RiderConfig setTriggerItem(@Nullable Item item) {
        this.triggerItem = item != null ? item : Items.AIR;
        return this;
    }

    public RiderConfig addDriverSlot(ResourceLocation slotId,
                                     List<Item> allowedItems,
                                     boolean isRequired,
                                     boolean allowReplace) {

        slotDefinitions.put(slotId,
                new SlotDefinition(allowedItems, null, allowReplace,  false, isRequired));

        if (isRequired) {
            requiredSlots.add(slotId);
        }
        return this;
    }

    public RiderConfig addForm(FormConfig form) {
        forms.put(form.getFormId(), form);
        if (baseFormId == null) {
            baseFormId = form.getFormId();
        }
        return this;
    }

    // 设置基础形态
    public void setBaseForm(ResourceLocation formId) {
        if (forms.containsKey(formId)) {
            baseFormId = formId;
        }
    }

    public RiderConfig addAuxSlot(ResourceLocation slotId, List<Item> allowedItems, boolean isRequired, boolean allowReplace) {
        auxSlotDefinitions.put(slotId, new SlotDefinition(allowedItems, null, allowReplace, true, isRequired));
        if (isRequired) {
            auxRequiredSlots.add(slotId);
        }
        return this;
    }

    // 设置辅助驱动器物品和装备槽位
    public RiderConfig setAuxDriverItem(Item item, EquipmentSlot slot) {
        this.auxDriverItem = item;
        this.auxDriverSlot = slot;
        return this;
    }

    // 形态匹配
    public ResourceLocation matchForm(Player player, Map<ResourceLocation, ItemStack> beltItems) {
        RideBattleLib.LOGGER.debug("开始匹配形态，玩家: {}", player.getName().getString());
        RideBattleLib.LOGGER.debug("当前腰带内容: {}", beltItems);
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return null;
        FormOverrideEvent overrideEvent = new FormOverrideEvent(player, beltItems, null);
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

        if (isBeltEmpty(beltItems)) {
            if (baseFormId != null && forms.containsKey(baseFormId) &&
                    forms.get(baseFormId).allowsEmptyBelt()) {
                RideBattleLib.LOGGER.debug("使用允许空腰带的基础形态: {}", baseFormId);
                return baseFormId;
            } else {
                RideBattleLib.LOGGER.warn("腰带为空，且没有允许空腰带的基础形态");
                return null;
            }
        }

        // 先检查是否所有“必需槽位”都有有效物品
        for (ResourceLocation slotId : requiredSlots) {
            SlotDefinition slot = getSlotDefinition(slotId);
            if (slot == null) continue;

            ItemStack stack = beltItems.get(slotId);
            if ((stack == null || stack.isEmpty()) && slot.isRequired()) {
                RideBattleLib.LOGGER.warn("必需槽位 {} 为空", slotId);
                return null; // 必需槽位不能为空
            }
        }

        // 检查辅助必需槽位
        for (ResourceLocation slotId : auxRequiredSlots) {
            SlotDefinition slot = getAuxSlotDefinition(slotId);
            if (slot == null) continue;

            ItemStack stack = beltItems.get(slotId);
            if ((stack == null || stack.isEmpty()) && slot.isRequired()) {
                RideBattleLib.LOGGER.warn("辅助必需槽位 {} 为空", slotId);
                return null; // 辅助必需槽位不能为空
            }
        }

        // 尝试匹配所有形态
        for (FormConfig formConfig : forms.values()) {
            boolean mainMatches = formConfig.matchesMainSlots(beltItems, config);
            boolean auxMatches = true;

            // 检查形态是否有辅助槽位要求
            boolean formHasAuxRequirements = !formConfig.getAuxRequiredItems().isEmpty();

            if (formHasAuxRequirements) {
                // 形态要求辅助槽位：必须装备辅助驱动器且槽位匹配
                if (hasAuxDriverEquipped(player)) {
                    auxMatches = formConfig.matchesAuxSlots(beltItems, config);
                } else {
                    auxMatches = false; // 未装备辅助驱动器但形态要求→不匹配
                    RideBattleLib.LOGGER.debug("形态{}需要辅助驱动器，但玩家未装备", formConfig.getFormId());
                }
            }

            if (mainMatches && auxMatches) {
                ResourceLocation formId = formConfig.getFormId();
                RideBattleLib.LOGGER.debug("匹配到的形态ID: {}", formId);
                return formId;
            }
        }

        // 回退到基础形态
        if (baseFormId != null && forms.containsKey(baseFormId) && forms.get(baseFormId).allowsEmptyBelt()) {
            // 仅当腰带为空时才使用基础形态
            if (isBeltEmpty(beltItems)) {
                return baseFormId;
            }
        }

        if (this.allowsDynamicForms()) {
            RideBattleLib.LOGGER.debug("未找到预设形态，尝试创建动态形态");
            try {
                FormConfig dynamicForm = DynamicFormManager.getOrCreateDynamicForm(player, this, beltItems);
                return dynamicForm.getFormId();
            } catch (Exception e) {
                RideBattleLib.LOGGER.error("动态形态创建失败", e);
            }
        } else {
            RideBattleLib.LOGGER.debug("该骑士不支持动态形态，跳过动态形态生成");
        }
        RideBattleLib.LOGGER.warn("未找到匹配形态，且没有允许空腰带的基础形态");
        return null;
    }

    // 快捷获取FormConfig
    public FormConfig getActiveFormConfig(Player player) {
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = matchForm(player, beltItems);

        // 优先检查预设形态
        if (forms.containsKey(formId)) {
            return forms.get(formId);
        }

        // 处理动态形态
        return DynamicFormManager.getDynamicForm(formId);
    }

    public RiderConfig setCommonArmor(EquipmentSlot slot, Item item) {
        commonArmorMap.put(slot, item);
        return this;
    }

    public RiderConfig setAllowDynamicForms(boolean allow) {
        this.allowDynamicForms = allow;
        return this;
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

    //通过玩家装备查找激活的驱动器配置
    public static RiderConfig findActiveDriverConfig(Player player) {
        for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
            // 精确匹配驱动器槽位和物品
            ItemStack driverStack = player.getItemBySlot(config.getDriverSlot());
            if (driverStack.is(config.getDriverItem())) {
                return config;
            }
        }
        return null;
    }

    //获取必要槽位列表
    public Set<ResourceLocation> getRequiredSlots() {
        return Collections.unmodifiableSet(requiredSlots);
    }

    //获取槽位定义
    public SlotDefinition getSlotDefinition(ResourceLocation slotId) {
        return slotDefinitions.get(slotId);
    }

    //获取所有槽位定义的不可修改视图
    public Map<ResourceLocation, SlotDefinition> getSlotDefinitions() {
        return Collections.unmodifiableMap(slotDefinitions);
    }

    // 添加形态获取方法
    public FormConfig getForms(ResourceLocation formId) {
        return forms.get(formId);
    }

    public ResourceLocation getBaseFormId() {
        return baseFormId;
    }

    private boolean isBeltEmpty(Map<ResourceLocation, ItemStack> beltItems) {
        if (beltItems.isEmpty()) return true;
        for (ItemStack stack : beltItems.values()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public boolean hasAuxDriverEquipped(Player player) {
        ItemStack auxStack = player.getItemBySlot(auxDriverSlot);
        return !auxStack.isEmpty() && auxStack.is(auxDriverItem);
    }

    public SlotDefinition getAuxSlotDefinition(ResourceLocation slotId) {
        return auxSlotDefinitions.get(slotId);
    }

    // 获取所有辅助驱动器槽位
    public Map<ResourceLocation, SlotDefinition> getAuxSlotDefinitions() {
        return Collections.unmodifiableMap(auxSlotDefinitions);
    }

    public Map<EquipmentSlot, Item> getCommonArmorMap() {
        return Collections.unmodifiableMap(commonArmorMap);
    }

    public boolean allowsDynamicForms() {
        return allowDynamicForms;
    }
}
