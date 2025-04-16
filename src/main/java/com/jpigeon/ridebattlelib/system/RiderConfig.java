package com.jpigeon.ridebattlelib.system;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class RiderConfig {
    private final ResourceLocation riderId; //定义骑士Id
    private Item driverItem;                //定义驱动器
    private EquipmentSlot driverSlot;       //定义驱动器位置
    private Item requiredItem;              //必要物品
    //定义盔甲
    private Item helmet;
    private Item chestplate;
    private Item leggings;
    private Item boots;


    //====================初始化方法====================

    //骑士Id初始化
    public RiderConfig(ResourceLocation riderId) {
        this.riderId = riderId;
    }


    //====================Getter方法====================

    //获取骑士Id
    public ResourceLocation getRiderId() {
        return riderId;
    }

    //获取驱动器物品
    public Item getDriverItem(){
        return driverItem;
    }

    //获取驱动器位置
    public EquipmentSlot getDriverSlot(){
        return driverSlot;
    }

    //获取必须物品
    public Item getRequiredItem() {
        return requiredItem;
    }

    //获取盔甲
    public Item getHelmet() {
        return helmet;
    }
    public Item getChestplate() {
        return chestplate;
    }
    public @Nullable Item getLeggings() {
        return leggings;
    }
    public Item getBoots() {
        return boots;
    }

    //====================Setter方法====================

    //指定驱动器物品
    public RiderConfig setDriverItem(Item item, EquipmentSlot slot){
        this.driverItem = item;
        this.driverSlot = slot;
        return this;
    }

    //指定需要物品
    public RiderConfig setRequiredItem(Item item) {
        this.requiredItem = item;
        return this;
    }

    //指定全身盔甲
    public RiderConfig setArmor(Item helmet, Item chestplate, Item leggings, Item boots){
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
        return this;
    }


}

