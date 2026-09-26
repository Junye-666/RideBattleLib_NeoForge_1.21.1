package com.jpigeon.ridebattlelib.common.api.server;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ServerRiderDispatcher {
    private static final Map<ResourceLocation, IRiderServerHandler> HANDLERS = new ConcurrentHashMap<>();

    public static void register(IRiderServerHandler handler) {
        HANDLERS.put(handler.riderId(), handler);
    }

    public static @Nullable IRiderServerHandler get(ResourceLocation riderId) {
        return riderId == null ? null : HANDLERS.get(riderId);
    }

    public static void dispatch(ResourceLocation riderId, Consumer<IRiderServerHandler> action) {
        IRiderServerHandler h = get(riderId);
        if (h != null) action.accept(h);
    }

    public static void clear() {
        HANDLERS.clear();
    }
}