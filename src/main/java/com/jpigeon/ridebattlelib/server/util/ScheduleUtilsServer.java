package com.jpigeon.ridebattlelib.server.util;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.util.ScheduleUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, value = Dist.DEDICATED_SERVER)
public final class ScheduleUtilsServer {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ScheduleUtils.getInstance().tick();
    }
}
