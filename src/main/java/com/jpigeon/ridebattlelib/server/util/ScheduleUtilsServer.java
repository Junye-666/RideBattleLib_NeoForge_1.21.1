package com.jpigeon.ridebattlelib.server.util;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.util.ScheduleUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public final class ScheduleUtilsServer {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ScheduleUtils.getInstance().tickServer();
    }
}
