package com.jpigeon.ridebattlelib.system.handler;

import com.jpigeon.ridebattlelib.system.rider.Henshin;
import com.jpigeon.ridebattlelib.system.rider.RiderConfig;
import com.jpigeon.ridebattlelib.system.rider.RiderRegistry;
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
        ItemStack usedItem = event.getItemStack();

        for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
            if (!usedItem.is(config.getRequiredItem())) continue;

            ItemStack driverStack = player.getItemBySlot(config.getDriverSlot());
            if (!driverStack.is(config.getDriverItem())) continue;

            Henshin.playerHenshin(player, config);
            event.setCanceled(true);
            break;
        }
    }
}
