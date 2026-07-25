package com.jpigeon.ridebattlelib.client.network;

import com.jpigeon.ridebattlelib.client.cache.ClientDriverDataCache;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.client.event.ClientRiderEvents;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataDiffPacket;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.common.network.packet.HenshinStateSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;
import java.util.UUID;

public class ClientPacketHandler {
    public static void handleHenshinStateSync(HenshinStateSyncPacket payload) {
        Minecraft.getInstance().execute(() -> {
            UUID playerId = payload.playerId();
            boolean wasTransformed = ClientTransformedCache.isTransformed(playerId);
            ResourceLocation oldForm = ClientTransformedCache.getCurrentFormId(playerId);

            ClientTransformedCache.update(
                    playerId,
                    payload.isTransformed(),
                    payload.state(),
                    payload.currentFormId(),
                    payload.pendingFormId()
            );

            ClientRiderEvents.HenshinStateChanged.ChangeType changeType;
            if (payload.state() == HenshinState.TRANSFORMING) {
                changeType = ClientRiderEvents.HenshinStateChanged.ChangeType.PENDING;
            } else if (payload.isTransformed() && !wasTransformed) {
                changeType = ClientRiderEvents.HenshinStateChanged.ChangeType.HENSHIN;
            } else if (!payload.isTransformed() && wasTransformed) {
                changeType = ClientRiderEvents.HenshinStateChanged.ChangeType.UNHENSHIN;
            } else if (payload.isTransformed() && !Objects.equals(payload.currentFormId(), oldForm)) {
                changeType = ClientRiderEvents.HenshinStateChanged.ChangeType.SWITCH;
            } else {
                // 单纯刷新，不发事件
                return;
            }

            Player player = Minecraft.getInstance().player;
            if (player != null) {
                NeoForge.EVENT_BUS.post(new ClientRiderEvents.HenshinStateChanged(
                        player,
                        payload.isTransformed(),
                        payload.riderId(),
                        payload.currentFormId(),
                        payload.pendingFormId(),
                        changeType
                ));
            }
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

            Player player = Minecraft.getInstance().player;
            if (player != null) {
                NeoForge.EVENT_BUS.post(new ClientRiderEvents.DriverDataChanged(
                        player,
                        payload.changes(),
                        payload.fullSync()
                ));
            }
        });
    }
}
