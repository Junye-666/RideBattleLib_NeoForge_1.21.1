package com.jpigeon.ridebattlelib.network.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.system.rider.Henshin;

import com.jpigeon.ridebattlelib.network.packet.UnhenshinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;


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

    public static void sendToServer(CustomPacketPayload packet){
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(packet);
    }
}
