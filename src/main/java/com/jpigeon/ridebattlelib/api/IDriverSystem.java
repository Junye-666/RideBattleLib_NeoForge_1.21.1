package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 驱动器系统
 * 管理驱动器内部物品的存储和操作
 */
public interface IDriverSystem {
    /**
     * 插入物品
     * @return 是否成功
     */
    boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack);

    /**
     * 取出单个槽位物品
     * @param slotId 槽位ID
     * @return ItemStack
     */
    ItemStack extractItem(Player player, ResourceLocation slotId);
    /**
     * 获取驱动器物品
     * @return 槽位物品
     */
    Map<ResourceLocation, ItemStack> getDriverItems(Player player);
    /**
     * 退回所有物品至玩家
     */
    void returnItems(Player player);
}
