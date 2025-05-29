package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IBeltSystem {
    // 槽位操作
    boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack);
    ItemStack extractItem(Player player, ResourceLocation slotId);

    // 物品匹配验证
    boolean validateItems(Player player, ResourceLocation riderId);

    // 获取当前腰带物品
    Map<ResourceLocation, ItemStack> getBeltItems(Player player);

    boolean isSlotOccupied(Player player, ResourceLocation slotId);
}
