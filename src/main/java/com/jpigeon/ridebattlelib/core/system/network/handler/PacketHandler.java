package com.jpigeon.ridebattlelib.core.system.network.handler;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import io.netty.handler.logging.LogLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("0.9.9.2")
                .playToServer(HenshinPacket.TYPE, HenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.INSTANCE.henshin(targetPlayer, payload.riderId());
                            }
                        })
                .playToServer(UnhenshinPacket.TYPE, UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.INSTANCE.unHenshin(targetPlayer);
                            }
                        })
                .playToServer(SwitchFormPacket.TYPE, SwitchFormPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.INSTANCE.switchForm(context.player(), payload.formId());
                            }
                        })
                .playToClient(DriverDataSyncPacket.TYPE, DriverDataSyncPacket.STREAM_CODEC,
                        (payload, context) ->
                                DriverSystem.INSTANCE.applySyncPacket(payload))
                .playToServer(ReturnItemsPacket.TYPE, ReturnItemsPacket.STREAM_CODEC,
                        (payload, context) ->
                                DriverSystem.INSTANCE.returnItems(context.player()))
                .playToServer(ExtractItemPacket.TYPE, ExtractItemPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                DriverSystem.INSTANCE.extractItem(context.player(), payload.slotId());
                            }
                        })

                .playToClient(DriverDataDiffPacket.TYPE, DriverDataDiffPacket.STREAM_CODEC,
                        (payload, context) -> DriverSystem.INSTANCE.applyDiffPacket(payload)
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
                .playToServer(
                        SyncHenshinStatePacket.TYPE,
                        SyncHenshinStatePacket.STREAM_CODEC,
                        (packet, context) -> {
                            Player player = context.player();
                            RiderData data = player.getData(RiderAttachments.RIDER_DATA);

                            // 应用新状态
                            data.setHenshinState(packet.state());
                            data.setPendingFormId(packet.pendingFormId());

                            if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                                RideBattleLib.LOGGER.debug("收到状态同步包: player={}, state={}, form={}",
                                        player.getName().getString(),
                                        packet.state(),
                                        packet.pendingFormId());
                            }
                            // 同步给所有客户端
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                HenshinSystem.syncHenshinState(serverPlayer);
                            } else if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)){
                                RideBattleLib.LOGGER.debug("玩家未连接: {}", player.getName().getString());
                            }
                        }
                )
                .playToServer(
                        RotateSkillPacket.TYPE,
                        RotateSkillPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 验证发送者身份
                            if (!payload.playerId().equals(context.player().getUUID()) && Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                                RideBattleLib.LOGGER.debug("RotateSkillPacket发送者身份不匹配: 预期={}, 实际={}",
                                        payload.playerId(), context.player().getUUID());
                            }

                            // 获取正确的玩家对象
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                SkillSystem.rotateSkill(targetPlayer);
                            }
                        }
                )
                .playToServer(
                        TriggerSkillPacket.TYPE,
                        TriggerSkillPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 验证发送者身份
                            if (!payload.playerId().equals(context.player().getUUID()) && Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                                RideBattleLib.LOGGER.debug("TriggerSkillPacket发送者身份不匹配: 预期={}, 实际={}",
                                        payload.playerId(), context.player().getUUID());
                            }

                            // 获取正确的玩家对象
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                SkillSystem.triggerCurrentSkill(targetPlayer);
                            }
                        }
                )
        ;
    }

    public static void sendToServer(CustomPacketPayload packet) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(packet);
        }
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        if (packet instanceof HenshinStateSyncPacket && Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)){
            RideBattleLib.LOGGER.debug("发送状态同步包");
        }
        player.connection.send(packet);
    }
}
