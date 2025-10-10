package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IDriverSystem {
    boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack);
    ItemStack extractItem(Player player, ResourceLocation slotId);
    Map<ResourceLocation, ItemStack> getDriverItems(Player player);
    void returnItems(Player player);
}
