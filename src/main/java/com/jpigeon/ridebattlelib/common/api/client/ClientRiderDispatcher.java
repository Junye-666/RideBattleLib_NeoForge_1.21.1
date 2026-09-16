package com.jpigeon.ridebattlelib.common.api.client;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ClientRiderDispatcher {
    private static final Map<ResourceLocation, IRiderClientHandler> HANDLERS = new ConcurrentHashMap<>();

    public static void register(IRiderClientHandler handler) {
        HANDLERS.put(handler.riderId(), handler);
    }

    public static @Nullable IRiderClientHandler get(ResourceLocation riderId) {
        return riderId == null ? null : HANDLERS.get(riderId);
    }

    public static void dispatch(@Nullable ResourceLocation riderId, Consumer<IRiderClientHandler> action) {
        IRiderClientHandler h = get(riderId);
        if (h != null) action.accept(h);
    }

    public static void clear() { HANDLERS.clear(); }
}