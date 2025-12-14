package com.jpigeon.ridebattlelib.core.system.skill;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public class SkillCooldownHandler {
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SkillSystem.clearPlayerCooldowns(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            SkillSystem.clearAllSkillCooldowns(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 在重生时清理冷却
        SkillSystem.clearAllSkillCooldowns(event.getEntity());
    }
}
