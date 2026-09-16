package com.jpigeon.ridebattlelib.common.network;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.client.network.ClientRiderSyncManager;
import com.jpigeon.ridebattlelib.common.network.packet.*;
import com.jpigeon.ridebattlelib.server.system.DriverSystem;
import com.jpigeon.ridebattlelib.server.system.HenshinSystem;
import com.jpigeon.ridebattlelib.server.system.SkillSystem;
import com.jpigeon.ridebattlelib.server.system.helper.DriverActionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("1.3.0").optional()
                .playToServer(DriverActionPacket.TYPE, DriverActionPacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.getInstance().driverAction(context.player()))
                .playToServer(HenshinPacket.TYPE, HenshinPacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.getInstance().henshin(context.player(), payload.riderId()))
                .playToServer(UnhenshinPacket.TYPE, UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.getInstance().unHenshin(context.player()))
                .playToServer(SwitchFormPacket.TYPE, SwitchFormPacket.STREAM_CODEC,
                        (payload, context) -> HenshinSystem.getInstance().switchForm(context.player(), payload.formId()))
                .playToServer(InsertItemPacket.TYPE, InsertItemPacket.STREAM_CODEC,
                        (payload, context) -> DriverSystem.getInstance().insertItem(context.player(), payload.slotId(), payload.stack()))
                .playToServer(ReturnItemsPacket.TYPE, ReturnItemsPacket.STREAM_CODEC,
                        (payload, context) -> DriverSystem.getInstance().returnItems(context.player()))
                .playToServer(ExtractItemPacket.TYPE, ExtractItemPacket.STREAM_CODEC,
                        (payload, context) -> DriverSystem.getInstance().extractItem(context.player(), payload.slotId()))
                .playToServer(RotateSkillPacket.TYPE, RotateSkillPacket.STREAM_CODEC,
                        (payload, context) -> SkillSystem.rotateSkill(context.player()))
                .playToServer(TriggerSkillPacket.TYPE, TriggerSkillPacket.STREAM_CODEC,
                        (payload, context) -> SkillSystem.triggerCurrentSkill(context.player()))
                .playToServer(SoundPacket.TYPE, SoundPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 服务端处理
                            ServerPlayer sender = (ServerPlayer) context.player();

                            // 获取音效
                            SoundEvent sound = payload.sound();
                            if (sound == null) return;

                            // 服务端广播
                            sender.level().playSound(null, sender, sound, SoundSource.PLAYERS, payload.volume(), payload.pitch());
                        }
                )
                .playToServer(CompleteHenshinPacket.TYPE, CompleteHenshinPacket.STREAM_CODEC,
                        (payload, context) -> DriverActionManager.getInstance().completeTransformation(context.player()))

                .playToClient(HenshinStateSyncPacket.TYPE, HenshinStateSyncPacket.STREAM_CODEC,
                        (payload, ctx) -> ClientRiderSyncManager.applyHenshinState(payload))
                .playToClient(DriverDataSyncPacket.TYPE, DriverDataSyncPacket.STREAM_CODEC,
                        (payload, ctx) -> ClientRiderSyncManager.applyDriverData(payload))
                .playToClient(DriverDataDiffPacket.TYPE, DriverDataDiffPacket.STREAM_CODEC,
                        (payload, ctx) -> ClientRiderSyncManager.applyDriverDiff(payload))
        ;
    }
}
