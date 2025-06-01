package com.jpigeon.ridebattlelib.core.system.penalty;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinCore;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public class CooldownHandler {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return; // 确保只在服务端执行

        Player player = event.getEntity();
        HenshinCore.updateCooldownEffects(player);
    }
}
