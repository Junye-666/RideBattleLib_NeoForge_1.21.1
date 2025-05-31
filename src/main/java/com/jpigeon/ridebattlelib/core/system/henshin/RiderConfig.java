package com.jpigeon.ridebattlelib.core.system.henshin;


import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.SlotDefinition;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    //定义骑士Id
    private final ResourceLocation riderId;
    //定义驱动器
    private Item driverItem = Items.AIR;
    //定义驱动器位置
    private EquipmentSlot driverSlot = EquipmentSlot.LEGS;
    //触发变身方式
    private TriggerType triggerType = TriggerType.KEY;
    private Item triggerItem = Items.AIR;
    private ResourceLocation baseFormId;
    //定义
    private final Map<ResourceLocation, SlotDefinition> slotDefinitions = new HashMap<>();
    private final Set<ResourceLocation> requiredSlots = new HashSet<>();
    final Map<ResourceLocation, FormConfig> forms = new HashMap<>();
    private final Map<EquipmentSlot, Item> universalGear = new EnumMap<>(EquipmentSlot.class);

    //====================初始化方法====================

    //骑士Id初始化
    public RiderConfig(ResourceLocation riderId) {
        this.riderId = riderId;
        // Arrays.fill(armor, Items.AIR);
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

    //获取驱动器位置
    public EquipmentSlot getDriverSlot() {
        return driverSlot;
    }

    //获取触发方式
    public TriggerType getTriggerType() {
        return triggerType;
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
    public FormConfig getForm(ResourceLocation formId) {
        return forms.get(formId);
    }

    // 通用装备
    public Map<EquipmentSlot, Item> getUniversalGear() {
        return Collections.unmodifiableMap(universalGear);
    }

    //====================Setter方法====================

    //指定驱动器物品
    public RiderConfig setDriverItem(Item item, EquipmentSlot slot) {
        this.driverItem = item;
        this.driverSlot = slot;
        return this;
    }

    //指定触发方式
    public RiderConfig setTriggerType(TriggerType type) {
        this.triggerType = type;
        return this;
    }

    //指定触发用物品
    public RiderConfig setTriggerItem(@Nullable Item item) {
        this.triggerItem = item != null ? item : Items.AIR;
        return this;
    }

    public RiderConfig addSlot(ResourceLocation slotId,
                               List<Item> allowedItems,
                               boolean isRequired,
                               boolean allowReplace) { // 新增参数

        slotDefinitions.put(slotId,
                new SlotDefinition(allowedItems, null, allowReplace)); // 传递新参数

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
    public RiderConfig setBaseForm(ResourceLocation formId) {
        if (forms.containsKey(formId)) {
            baseFormId = formId;
        }
        return this;
    }

    // 形态匹配
    public ResourceLocation matchForm(Map<ResourceLocation, ItemStack> beltItems) {
        if (beltItems == null || beltItems.isEmpty()) {
            return baseFormId; // 返回基础形态
        }

        beltItems.forEach((id, stack) ->
                RideBattleLib.LOGGER.info("- {}: {} x{}", id, stack.getItem().getDescriptionId(), stack.getCount()));
        // 优先匹配
        for (FormConfig form : forms.values()) {
            if (form.matches(beltItems)) {
                RideBattleLib.LOGGER.info("匹配到固定形态: {}", form.getFormId());
                return form.getFormId();
            }
        }

        // 其次匹配
        for (FormConfig form : forms.values()) {
            if (!form.dynamicParts.isEmpty() && form.matchesDynamic(beltItems)) {
                return form.getDynamicFormId(beltItems);
            }
        }

        // 回退到基础形态
        RideBattleLib.LOGGER.info("未匹配到形态，使用基础形态: {}", baseFormId);
        return baseFormId;
    }

    public RiderConfig setUniversalGear(EquipmentSlot slot, Item item) {
        universalGear.put(slot, item);
        return this;
    }
}
