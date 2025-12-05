package com.jpigeon.ridebattlelib.core.system.network.handler;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.SyncManager;
import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("1.0.2")
                .playToServer(DriverActionPacket.TYPE, DriverActionPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.INSTANCE.driverAction(targetPlayer);
                            }
                        })
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
                .playToServer(InsertItemPacket.TYPE, InsertItemPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                DriverSystem.INSTANCE.insertItem(targetPlayer, payload.slotId(), payload.stack());
                            }
                        })
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
                                SyncManager.INSTANCE.syncTransformedState(serverPlayer);
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

                            if (Config.DEBUG_MODE.get()) {
                                RideBattleLib.LOGGER.debug("收到状态同步包: player={}, state={}, form={}",
                                        player.getName().getString(),
                                        packet.state(),
                                        packet.pendingFormId());
                            }
                            // 同步给所有客户端
                            if (context.player() instanceof ServerPlayer serverPlayer) {
                                SyncManager.INSTANCE.syncHenshinState(serverPlayer);
                            } else if (Config.DEBUG_MODE.get()){
                                RideBattleLib.LOGGER.debug("玩家未连接: {}", player.getName().getString());
                            }
                        }
                )
                .playToServer(
                        RotateSkillPacket.TYPE,
                        RotateSkillPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 验证发送者身份
                            if (!payload.playerId().equals(context.player().getUUID()) && Config.DEBUG_MODE.get()) {
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
                            if (!payload.playerId().equals(context.player().getUUID()) && Config.DEBUG_MODE.get()) {
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
        if (packet instanceof HenshinStateSyncPacket && Config.DEBUG_MODE.get()){
            RideBattleLib.LOGGER.debug("发送状态同步包");
        }
        player.connection.send(packet);
    }
}
