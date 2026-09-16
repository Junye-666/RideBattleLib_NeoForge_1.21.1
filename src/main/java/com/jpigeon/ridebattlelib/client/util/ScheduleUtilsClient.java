package com.jpigeon.ridebattlelib.client.util;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.util.ScheduleUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, value = Dist.CLIENT)
public final class ScheduleUtilsClient {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ScheduleUtils.getInstance().tick();
    }
}
