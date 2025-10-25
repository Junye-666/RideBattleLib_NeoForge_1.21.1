package com.jpigeon.ridebattlelib.core.system.penalty;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public class PenaltyHandler {
    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 是否需要触发吃瘪
        if (PenaltySystem.shouldTriggerPenalty(player)) {
            PenaltySystem.PENALTY_SYSTEM.penaltyUnhenshin(player);

            // 设生命值为安全值
            player.setHealth(6.0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 强制解除
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            HenshinSystem.INSTANCE.unHenshin(player);
        }

        player.removeTag("penalty_cooldown");
    }
}
