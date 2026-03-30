package com.jpigeon.ridebattlelib.client.network;

import com.jpigeon.ridebattlelib.client.cache.ClientDriverDataCache;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataDiffPacket;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.common.network.packet.HenshinStateSyncPacket;
import net.minecraft.client.Minecraft;

public class ClientPacketHandler {
    public static void handleHenshinStateSync(HenshinStateSyncPacket payload) {
        Minecraft.getInstance().execute(() -> {
            ClientTransformedCache.update(
                    payload.playerId(),
                    payload.isTransformed(),
                    payload.state(),
                    payload.currentFormId(),
                    payload.pendingFormId()
            );
        });
    }

    public static void handleDriverDataSync(DriverDataSyncPacket payload) {
        Minecraft.getInstance().execute(() -> {
            ClientDriverDataCache.setMainItems(payload.playerId(), payload.mainItems());
            ClientDriverDataCache.setAuxItems(payload.playerId(), payload.auxItems());
        });
    }

    public static void handleDriverDataDiff(DriverDataDiffPacket payload) {
        Minecraft.getInstance().execute(() -> {
            ClientDriverDataCache.applyChanges(payload.playerId(), payload.changes(), payload.fullSync());
        });
    }
}
