package com.jpigeon.ridebattlelib.server.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.system.DriverSystem;
import com.jpigeon.ridebattlelib.server.system.HenshinSystem;
import com.jpigeon.ridebattlelib.server.system.PenaltySystem;
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
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        // 是否需要触发吃瘪
        if (config.getPenaltyStrategy().shouldTriggerPenalty(player)) {
            PenaltySystem.getInstance().penaltyUnhenshin(player);

        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 强制解除
        if (HenshinUtils.isTransformed(player)) {
            HenshinSystem.getInstance().unHenshin(player);
        }

        if (!DriverSystem.getInstance().getDriverItems(player).isEmpty()){
            DriverSystem.getInstance().returnItems(player);
        }

        player.removeTag("penalty_cooldown");
    }
}
