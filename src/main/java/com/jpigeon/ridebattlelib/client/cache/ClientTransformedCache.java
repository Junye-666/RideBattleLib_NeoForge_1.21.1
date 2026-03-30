package com.jpigeon.ridebattlelib.client.cache;

import com.jpigeon.ridebattlelib.common.data.HenshinState;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientTransformedCache {
    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private record CacheEntry(
            boolean isTransformed,
            HenshinState state,
            @Nullable ResourceLocation currentFormId,
            @Nullable ResourceLocation pendingFormId
    ) {}

    public static void update(UUID playerId, boolean isTransformed, HenshinState state,
                              @Nullable ResourceLocation currentFormId,
                              @Nullable ResourceLocation pendingFormId) {
        CACHE.put(playerId, new CacheEntry(isTransformed, state, currentFormId, pendingFormId));
    }

    public static boolean isTransformed(UUID playerId) {
        CacheEntry entry = CACHE.get(playerId);
        if (entry == null) return false;
        return entry.isTransformed();
    }

    public static @Nullable ResourceLocation getCurrentFormId(UUID playerId) {
        CacheEntry entry = CACHE.get(playerId);
        return entry != null ? entry.currentFormId() : null;
    }

    public static @Nullable ResourceLocation getPendingFormId(UUID playerId) {
        CacheEntry entry = CACHE.get(playerId);
        return entry != null ? entry.pendingFormId() : null;
    }

    public static HenshinState getState(UUID playerId) {
        CacheEntry entry = CACHE.get(playerId);
        return entry != null ? entry.state() : HenshinState.IDLE;
    }

    public static void remove(UUID playerId) {
        CACHE.remove(playerId);
    }

    public static void clear() {
        CACHE.clear();
    }
}
