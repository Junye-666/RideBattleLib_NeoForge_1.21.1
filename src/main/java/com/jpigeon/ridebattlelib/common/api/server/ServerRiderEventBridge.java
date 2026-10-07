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

import java.util.function.Consumer;

@EventBusSubscriber(modid = RideBattleLib.MODID)
public final class ServerRiderEventBridge {

    /**
     * 从 player 反查 config 后派发；找不到 config 直接静默 return。
     */
    private static void dispatchByPlayer(Player player, Consumer<IRiderServerHandler> action) {
        if (player == null) return;
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        ServerRiderDispatcher.dispatch(config.getRiderId(), action);
    }

    // ==================== Henshin ====================

    @SubscribeEvent
    public static void onHenshinPre(HenshinEvent.Pre event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onHenshinPre(event));
    }

    @SubscribeEvent
    public static void onHenshinPost(HenshinEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onHenshinPost(event));
    }

    // ==================== FormSwitch ====================

    @SubscribeEvent
    public static void onSwitchPre(FormSwitchEvent.Pre event) {
        dispatchByPlayer(event.getPlayer(), h -> h.onSwitchPre(event));
    }

    @SubscribeEvent
    public static void onSwitchPost(FormSwitchEvent.Post event) {
        dispatchByPlayer(event.getPlayer(), h -> h.onSwitchPost(event));
    }

    // ==================== Unhenshin ====================

    @SubscribeEvent
    public static void onUnhenshinPre(UnhenshinEvent.Pre event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onUnhenshinPre(event));
    }

    @SubscribeEvent
    public static void onUnhenshinPost(UnhenshinEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getRiderId(), h -> h.onUnhenshinPost(event));
    }

    // ==================== Driver item insert / extract ====================

    @SubscribeEvent
    public static void onInsert(ItemInsertionEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getConfig().getRiderId(), h -> h.onInsert(event));
    }

    @SubscribeEvent
    public static void onExtract(SlotExtractionEvent.Post event) {
        ServerRiderDispatcher.dispatch(event.getConfig().getRiderId(), h -> h.onExtract(event));
    }

    // ==================== Skill ====================

    @SubscribeEvent
    public static void onSkillPre(SkillEvent.Pre event) {
        dispatchByPlayer(event.getPlayer(), h -> h.onSkillPre(event));
    }

    @SubscribeEvent
    public static void onSkillPost(SkillEvent.Post event) {
        Player player = event.getPlayer();
        dispatchByPlayer(player, h -> h.onSkillPost(event));

        if (player instanceof ServerPlayer sp) {
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config == null) return;
            PacketDistributor.sendToPlayer(
                    sp,
                    new SkillSyncPacket(sp.getUUID(), config.getRiderId(), event.getSkillId())
            );
        }
    }
}