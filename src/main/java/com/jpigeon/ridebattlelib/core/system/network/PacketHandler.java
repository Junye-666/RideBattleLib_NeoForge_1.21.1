package com.jpigeon.ridebattlelib.core.system.network;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.HenshinContext;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("1.1.0")
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
                                HenshinSystem.getInstance().switchForm(targetPlayer, payload.formId());
                            }
                        })
                .playToClient(DriverDataSyncPacket.TYPE, DriverDataSyncPacket.STREAM_CODEC,
                        (payload, context) ->
                                DriverSystem.getInstance().applySyncPacket(payload))
                .playToServer(InsertItemPacket.TYPE, InsertItemPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                DriverSystem.getInstance().insertItem(targetPlayer, payload.slotId(), payload.stack());
                            }
                        })
                .playToServer(ReturnItemsPacket.TYPE, ReturnItemsPacket.STREAM_CODEC,
                        (payload, context) ->
                                DriverSystem.getInstance().returnItems(context.player()))
                .playToServer(ExtractItemPacket.TYPE, ExtractItemPacket.STREAM_CODEC,
                        (payload, context) -> {
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                DriverSystem.getInstance().extractItem(targetPlayer, payload.slotId());
                            }
                        })

                .playToClient(DriverDataDiffPacket.TYPE, DriverDataDiffPacket.STREAM_CODEC,
                        (payload, context) -> DriverSystem.getInstance().applyDiffPacket(payload)
                )
                .playToClient(TransformedStatePacket.TYPE, TransformedStatePacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.CLIENT_TRANSFORMED_CACHE.put(payload.playerId(), payload.isTransformed()))
                .playToClient(HenshinStateSyncPacket.TYPE, HenshinStateSyncPacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.CLIENT_TRANSFORMED_CACHE.put(payload.playerId(), payload.state() == HenshinState.TRANSFORMED // 根据状态设置
                        ))
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
                                HenshinContext.DATA_SYNC.syncHenshinState(serverPlayer);
                            } else if (Config.DEBUG_MODE.get()) {
                                RideBattleLib.LOGGER.debug("玩家未连接: {}", player.getName().getString());
                            }
                        }
                )
                .playToServer(
                        RotateSkillPacket.TYPE,
                        RotateSkillPacket.STREAM_CODEC,
                        (payload, context) -> {
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
                            // 获取正确的玩家对象
                            Player targetPlayer = context.player().level().getPlayerByUUID(payload.playerId());
                            if (targetPlayer != null) {
                                SkillSystem.triggerCurrentSkill(targetPlayer);
                            }
                        }
                )
                .playToServer(
                        SoundPacket.TYPE,
                        SoundPacket.STREAM_CODEC,
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
        ;
    }
}
