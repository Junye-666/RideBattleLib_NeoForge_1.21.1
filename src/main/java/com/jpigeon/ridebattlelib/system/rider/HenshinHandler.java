package com.jpigeon.ridebattlelib.system.rider;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;


public class HenshinHandler {
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) return;

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();

        for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
            // 检查是否持有需求物品
            if (!heldItem.is(config.getRequiredItem())) continue;

            // 检查是否穿戴了驱动器物品
            ItemStack driverStack = player.getItemBySlot(config.getDriverSlot());
            if (!driverStack.is(config.getDriverItem())) continue;

            // 满足条件则变身
            Henshin.playerHenshin(player, config);
            event.setCanceled(true);
            return;
        }
    }
}
