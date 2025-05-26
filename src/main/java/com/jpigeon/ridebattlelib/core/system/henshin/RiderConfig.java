package com.jpigeon.ridebattlelib.core.system.henshin;


import com.jpigeon.ridebattlelib.core.system.belt.SlotDefinition;
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
    //定义盔甲
    private final Item[] armor = new Item[4];
    //定义
    private final Map<ResourceLocation, SlotDefinition> slotDefinitions = new HashMap<>();
    private final Set<ResourceLocation> requiredSlots = new HashSet<>();

    //====================初始化方法====================

    //骑士Id初始化
    public RiderConfig(ResourceLocation riderId) {
        this.riderId = riderId;
        Arrays.fill(armor, Items.AIR);
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



    //获取盔甲
    public Item getArmorPiece(EquipmentSlot slot) {
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return Items.AIR;
        }

        return switch (slot) {
            case HEAD -> armor[0];
            case CHEST -> armor[1];
            case LEGS -> armor[2];
            case FEET -> armor[3];
            default -> Items.AIR;
        };
    }

    public Item getHelmet() {
        return armor[0];
    }

    public Item getChestplate() {
        return armor[1];
    }

    public Item getLegging() {
        return armor[2];
    }

    public Item getBoots() {
        return armor[3];
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

    //指定全身盔甲
    public RiderConfig setArmor(Item helmet, Item chestplate, @Nullable Item leggings, Item boots) {
        armor[0] = helmet;
        armor[1] = chestplate;
        armor[2] = leggings != null ? leggings : Items.AIR;
        armor[3] = boots;
        return this;
    }

    //添加槽位

    public RiderConfig addSlot(ResourceLocation slotId, List<Item> allowedItems, boolean isRequired) {
        slotDefinitions.put(slotId, new SlotDefinition(allowedItems, null));
        if (isRequired) {
            requiredSlots.add(slotId); // 确保必要槽位被添加
        }
        return this;
    }
}

