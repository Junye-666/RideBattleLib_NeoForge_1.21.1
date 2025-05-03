package com.jpigeon.ridebattlelib.system.rider.upgrade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/* TODO:
    public class FormUpgrader {
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

            FormRegistry.FORMS.values().forEach(form -> {
            if (stack.getItem() == form.getRequiredItem()) {
                if (canUpgradeform(player, form)) {
                    playerUpgrade(player, form);
                }
            }
        });
    }

    private static boolean canUpgradeform(Player player, RiderForm form) {
    }

}
*/