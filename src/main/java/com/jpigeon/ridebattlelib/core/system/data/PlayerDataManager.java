package com.jpigeon.ridebattlelib.core.system.data;

import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Map<UUID, PlayerPersistentData> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();

    public static PlayerPersistentData getData(Player player) {
        return PLAYER_DATA_CACHE.computeIfAbsent(
                player.getUUID(),
                k -> player.getData(ModAttachments.PLAYER_DATA)
        );
    }

    public static void syncToClient(ServerPlayer player) {
        // 优化后的数据同步逻辑
    }
}
