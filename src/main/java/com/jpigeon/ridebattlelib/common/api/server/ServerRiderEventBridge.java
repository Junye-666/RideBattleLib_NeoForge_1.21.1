package com.jpigeon.ridebattlelib.common.api.server;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.network.packet.SkillSyncPacket;
import com.jpigeon.ridebattlelib.server.event.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public final class ServerRiderEventBridge {

    @SubscribeEvent
    public static void onHenshinPre(HenshinEvent.Pre event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onHenshinPre(event));
    }

    @SubscribeEvent
    public static void onHenshinPost(HenshinEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onHenshinPost(event));
    }

    @SubscribeEvent
    public static void onSwitchPre(FormSwitchEvent.Pre event) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(event.getPlayer());
        if (config == null) return;
        ServerRiderDispatcher.dispatch(config.getRiderId(), h -> h.onSwitchPre(event));
    }

    @SubscribeEvent
    public static void onSwitchPost(FormSwitchEvent.Post event) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(event.getPlayer());
        if (config == null) return;
        ServerRiderDispatcher.dispatch(config.getRiderId(), h -> h.onSwitchPost(event));
    }

    @SubscribeEvent
    public static void onUnhenshinPre(UnhenshinEvent.Pre event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onUnhenshinPre(event));
    }

    @SubscribeEvent
    public static void onUnhenshinPost(UnhenshinEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onUnhenshinPost(event));
    }

    @SubscribeEvent
    public static void onInsert(ItemInsertionEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getConfig().getRiderId(), h -> h.onInsert(event));
    }

    @SubscribeEvent
    public static void onExtract(SlotExtractionEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getConfig().getRiderId(), h -> h.onExtract(event));
    }

    @SubscribeEvent
    public static void onSkillPre(SkillEvent.Pre event) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(event.getPlayer());
        if (config == null) return;
        ServerRiderDispatcher.dispatch(config.getRiderId(), h -> h.onSkillPre(event));
    }

    @SubscribeEvent
    public static void onSkillPost(SkillEvent.Post event) {
        Player player = event.getPlayer();
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(
                    sp,
                    new SkillSyncPacket(sp.getUUID(), config.getRiderId(), event.getSkillId())
            );
        }
        ServerRiderDispatcher.dispatch(config.getRiderId(), h -> h.onSkillPost(event));
    }
}
