package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public class RespawnHandler {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        player.removeTag("just_respawned");

        player.removeTag("penalty_cooldown");

        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            HenshinSystem.INSTANCE.unHenshin(player);
        }
    }
}
