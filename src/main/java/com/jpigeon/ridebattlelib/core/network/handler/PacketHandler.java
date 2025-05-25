package com.jpigeon.ridebattlelib.core.network.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.network.packet.UnhenshinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;


public class PacketHandler {
    private static final HenshinSystem HENSHIN_SYSTEM = new HenshinSystem();

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("0.1.0")
                .playToServer(
                        HenshinPacket.TYPE,
                        HenshinPacket.STREAM_CODEC,
                        (payload, context) ->
                        {
                            ServerPlayer player = (ServerPlayer) context.player();
                            if (player != null) {
                                HENSHIN_SYSTEM.henshin(context.player(), payload.riderId());
                            }

                        }
                )
                .playToServer(
                        UnhenshinPacket.TYPE,
                        UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            ServerPlayer player = (ServerPlayer) context.player();
                            if (player != null) {
                                HENSHIN_SYSTEM.unHenshin(player);
                            }
                        }
                );
        ;
    }

    public static void sendToServer(CustomPacketPayload packet) {
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(packet);
    }
}
