package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public class EventHandlerDemo {

    @SubscribeEvent
    public static void onHenshinPre(HenshinEvent.Pre event) {
        Player player = event.getPlayer();
        RideBattleLib.LOGGER.info("变身即将开始: {} -> {}",
                player.getName(), event.getRiderId());

        // 示例：检查权限
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("权限不足，无法变身"), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHenshinPost(HenshinEvent.Post event) {
        Player player = event.getPlayer();
        RideBattleLib.LOGGER.info("变身完成: {} -> {}",
                player.getName(), event.getRiderId());
    }

    @SubscribeEvent
    public static void onUnhenshinPre(UnhenshinEvent.Pre event) {
        Player player = event.getPlayer();
        RideBattleLib.LOGGER.info("解除变身即将开始: {}", player.getName());
    }

    @SubscribeEvent
    public static void onUnhenshinPost(UnhenshinEvent.Post event) {
        Player player = event.getPlayer();
        RideBattleLib.LOGGER.info("解除变身完成: {}", player.getName());
    }
}
