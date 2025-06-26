package com.jpigeon.ridebattlelib.core.system.network.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.network.packet.BeltDataSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("0.9.1")
                .playToServer(HenshinPacket.TYPE, HenshinPacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.INSTANCE.henshin(context.player(), payload.riderId()))
                .playToServer(UnhenshinPacket.TYPE, UnhenshinPacket.STREAM_CODEC,
                        (payload, context) ->
                                HenshinSystem.INSTANCE.unHenshin(context.player()))
                .playToServer(SwitchFormPacket.TYPE, SwitchFormPacket.STREAM_CODEC,
                        (payload, context) ->
                                HenshinSystem.INSTANCE.switchForm(context.player(), payload.formId()))
                .playToClient(BeltDataSyncPacket.TYPE, BeltDataSyncPacket.STREAM_CODEC,
                        (payload, context) ->
                                BeltSystem.INSTANCE.setBeltItems(context.player(), payload.items()))
                .playToServer(ReturnItemsPacket.TYPE, ReturnItemsPacket.STREAM_CODEC,
                        (payload, context) ->
                                BeltSystem.INSTANCE.returnItems(context.player()))
                .playToClient(BeltDataDiffPacket.TYPE, BeltDataDiffPacket.STREAM_CODEC,
                        (payload, context) -> BeltSystem.INSTANCE.applyDiffPacket(payload)
                )
                .playToClient(TransformedStatePacket.TYPE, TransformedStatePacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.CLIENT_TRANSFORMED_CACHE.put(payload.playerId(), payload.isTransformed()))
                .playToClient(HenshinStateSyncPacket.TYPE, HenshinStateSyncPacket.STREAM_CODEC,
                        (payload, context) ->
                        {
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                HenshinSystem.syncTransformedState(serverPlayer);
                            }
                        })

        ;
    }

    public static void sendToServer(CustomPacketPayload packet) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(packet);
        }
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        if (packet instanceof HenshinStateSyncPacket statePacket) {
            RideBattleLib.LOGGER.info("发送状态包: player={}, state={}, pendingForm={}",
                    player.getName().getString(), statePacket.state(), statePacket.pendingFormId());
        }
        player.connection.send(packet);
    }
}
