package com.jpigeon.ridebattlelib.core.system.henshin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;


public class HenshinHandler {
    private static final HenshinSystem HENSHIN_SYSTEM = new HenshinSystem(); // 创建静态实例

    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) return;

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();

        for (RiderConfig config : RiderRegistry.getRegisteredRiders()) {
            // 检查是否可变身
            if (!HENSHIN_SYSTEM.canTransform(player, config)) continue;

            // 满足条件则变身
            HENSHIN_SYSTEM.henshin(player, config.getRiderId());
            event.setCanceled(true);
            return;
        }
    }
}
