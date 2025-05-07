package com.jpigeon.ridebattlelib.system.rider.basic;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;


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
    //必要物品
    private Item requiredItem = Items.AIR;
    //定义盔甲
    private final Item[] armor = new Item[4];



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
    public Item getDriverItem(){
        return driverItem;
    }

    //获取驱动器位置
    public EquipmentSlot getDriverSlot(){
        return driverSlot;
    }

    //获取必须物品
    public @Nullable Item getRequiredItem() {
        return requiredItem;
    }

    //获取盔甲
    public Item getArmorPiece(EquipmentSlot slot){
        return armor[slot.getIndex() -2];
    }

    public Item getHelmet() {
        return armor[0];
    }

    public Item getChestplate() {
        return armor[1];
    }

    public Item getLegging(){
        return armor[2];
    }

    public Item getBoots() {
        return armor[3];
    }


    //====================Setter方法====================

    //指定驱动器物品
    public RiderConfig setDriverItem(Item item, EquipmentSlot slot){
        this.driverItem = item;
        this.driverSlot = slot;
        return this;
    }

    //指定需要物品
    public RiderConfig setRequiredItem(@Nullable Item item) {
        this.requiredItem = item != null ? item : Items.AIR;
        return this;
    }

    //指定全身盔甲
    public RiderConfig setArmor(Item helmet, Item chestplate, @Nullable Item leggings, Item boots){
        armor[0] = helmet;
        armor[1] = chestplate;
        armor[2] = leggings != null ? leggings : Items.AIR;
        armor[3] = boots;
        return this;
    }



}

