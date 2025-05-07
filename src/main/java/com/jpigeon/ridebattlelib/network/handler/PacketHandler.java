package com.jpigeon.ridebattlelib.network.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.system.rider.basic.Henshin;
import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;

import com.jpigeon.ridebattlelib.network.packet.UnhenshinPacket;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("0.0.3")
                .playToServer(
                        UnhenshinPacket.TYPE,
                        UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Henshin.playerUnhenshin(context.player());
                        }
                );
    }
}
