package com.jpigeon.ridebattlelib.common.network;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.client.network.ClientPacketHandler;
import com.jpigeon.ridebattlelib.common.network.packet.*;
import com.jpigeon.ridebattlelib.server.system.DriverSystem;
import com.jpigeon.ridebattlelib.server.system.HenshinSystem;
import com.jpigeon.ridebattlelib.server.system.SkillSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("1.2.4")
                .playToServer(DriverActionPacket.TYPE, DriverActionPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.getInstance().driverAction(targetPlayer);
                            }
                        })
                .playToServer(HenshinPacket.TYPE, HenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.getInstance().henshin(targetPlayer, payload.riderId());
                            }
                        })
                .playToServer(UnhenshinPacket.TYPE, UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.getInstance().unHenshin(targetPlayer);
                            }
                        })
                .playToServer(SwitchFormPacket.TYPE, SwitchFormPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                HenshinSystem.getInstance().switchForm(context.player(), payload.formId());
                            }
                        })
                .playToServer(InsertItemPacket.TYPE, InsertItemPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                DriverSystem.getInstance().insertItem(targetPlayer, payload.slotId(), payload.stack());
                            }
                        })
                .playToServer(ReturnItemsPacket.TYPE, ReturnItemsPacket.STREAM_CODEC,
                        (payload, context) -> DriverSystem.getInstance().returnItems(context.player()))
                .playToServer(ExtractItemPacket.TYPE, ExtractItemPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                DriverSystem.getInstance().extractItem(context.player(), payload.slotId());
                            }
                        })
                .playToServer(
                        RotateSkillPacket.TYPE, RotateSkillPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 获取正确的玩家对象
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                SkillSystem.rotateSkill(targetPlayer);
                            }
                        }
                )
                .playToServer(
                        TriggerSkillPacket.TYPE, TriggerSkillPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 获取正确的玩家对象
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                SkillSystem.triggerCurrentSkill(targetPlayer);
                            }
                        }
                )
                .playToServer(
                        SoundPacket.TYPE, SoundPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 服务端处理
                            ServerPlayer sender = (ServerPlayer) context.player();

                            if (!sender.getUUID().equals(payload.playerId())) return;

                            // 获取音效
                            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(payload.soundId());
                            if (sound == null) return;

                            // 服务端广播
                            sender.level().playSound(null, sender, sound, SoundSource.PLAYERS, payload.volume(), payload.pitch());
                        }
                )

                .playToClient(HenshinStateSyncPacket.TYPE, HenshinStateSyncPacket.STREAM_CODEC,
                        (payload, context) -> ClientPacketHandler.handleHenshinStateSync(payload))

                .playToClient(DriverDataSyncPacket.TYPE, DriverDataSyncPacket.STREAM_CODEC,
                        (payload, context) -> ClientPacketHandler.handleDriverDataSync(payload))

                .playToClient(DriverDataDiffPacket.TYPE, DriverDataDiffPacket.STREAM_CODEC,
                        (payload, context) -> ClientPacketHandler.handleDriverDataDiff(payload))
        ;
    }
}
