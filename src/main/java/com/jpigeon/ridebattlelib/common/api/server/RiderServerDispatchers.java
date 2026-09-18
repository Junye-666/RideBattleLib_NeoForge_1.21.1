package com.jpigeon.ridebattlelib.common.api.server;

import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.server.event.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RiderServerDispatchers {
    private static final Map<ResourceLocation, IRiderServerHandler> HANDLERS = new ConcurrentHashMap<>();

    public static void register(IRiderServerHandler handler) {
        HANDLERS.put(handler.riderId(), handler);
    }

    public static @Nullable IRiderServerHandler get(ResourceLocation riderId) {
        return riderId == null ? null : HANDLERS.get(riderId);
    }

    // ========== 事件转发 ==========

    @SubscribeEvent
    public static void onHenshinPre(HenshinEvent.Pre event) {
        dispatch(event.getRiderId(), h -> h.onHenshinPre(event));
    }

    @SubscribeEvent
    public static void onHenshinPost(HenshinEvent.Post event) {
        dispatch(event.getRiderId(), h -> h.onHenshinPost(event));
    }

    @SubscribeEvent
    public static void onSwitchPre(FormSwitchEvent.Pre event) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(event.getPlayer());
        if (config == null) return;
        dispatch(config.getRiderId(), h -> h.onSwitchPre(event));
    }

    @SubscribeEvent
    public static void onSwitchPost(FormSwitchEvent.Post event) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(event.getPlayer());
        if (config == null) return;
        dispatch(config.getRiderId(), h -> h.onSwitchPost(event));
    }

    @SubscribeEvent
    public static void onUnhenshinPre(UnhenshinEvent.Pre event) {
        dispatch(event.getRiderId(), h -> h.onUnhenshinPre(event));
    }

    @SubscribeEvent
    public static void onUnhenshinPost(UnhenshinEvent.Post event) {
        dispatch(event.getRiderId(), h -> h.onUnhenshinPost(event));
    }

    @SubscribeEvent
    public static void onInsert(ItemInsertionEvent.Post event) {
        dispatch(event.getConfig().getRiderId(), h -> h.onInsert(event));
    }

    @SubscribeEvent
    public static void onExtract(SlotExtractionEvent.Post event) {
        dispatch(event.getConfig().getRiderId(), h -> h.onExtract(event));
    }

    public static void dispatch(ResourceLocation riderId, Consumer<IRiderServerHandler> action) {
        IRiderServerHandler h = get(riderId);
        if (h != null) action.accept(h);
    }
}