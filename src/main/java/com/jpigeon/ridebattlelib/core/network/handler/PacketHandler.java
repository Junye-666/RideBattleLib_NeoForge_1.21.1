package com.jpigeon.ridebattlelib.core.network.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.core.network.packet.BeltDataSyncPacket;
import com.jpigeon.ridebattlelib.core.network.packet.ReturnItemsPacket;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
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
                .versioned("0.2.0")
                .playToServer(
                        HenshinPacket.TYPE,
                        HenshinPacket.STREAM_CODEC,
                        (payload, context) ->
                        {
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                HENSHIN_SYSTEM.henshin(context.player(), payload.riderId());
                            }

                        }
                )
                .playToServer(
                        UnhenshinPacket.TYPE,
                        UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof ServerPlayer) {
                                HENSHIN_SYSTEM.unHenshin(context.player());
                            }
                        }
                )
                .playToClient(
                        BeltDataSyncPacket.TYPE,
                        BeltDataSyncPacket.STREAM_CODEC,
                        (payload, context) -> {
                            if (Minecraft.getInstance().player != null) {
                                BeltSystem.beltData.put(payload.playerId(), payload.items());
                            }
                        }
                )
                .playToServer(
                        ReturnItemsPacket.TYPE,
                        ReturnItemsPacket.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                BeltSystem.INSTANCE.returnItems(serverPlayer);
                            }
                        }
                )

        ;
    }

    public static void sendToServer(CustomPacketPayload packet) {
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(packet);
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        player.connection.send(packet);
    }
}
