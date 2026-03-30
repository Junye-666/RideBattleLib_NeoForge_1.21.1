package com.jpigeon.ridebattlelib.server.system.helper;

import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.common.network.packet.HenshinStateSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class SyncManager {
    private static final SyncManager INSTANCE = new SyncManager();
    public static SyncManager getInstance() {
        return INSTANCE;
    }

    /**
     * 同步玩家所有相关状态
     */
    public void syncAllPlayerData(ServerPlayer player) {
        syncHenshinState(player);
        syncDriverData(player);
    }

    /**
     * 同步变身状态
     */
    public void syncHenshinState(ServerPlayer player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        HenshinSessionData session = data.getSessionData();
        HenshinStateSyncPacket stateSyncPacket = new HenshinStateSyncPacket(
                player.getUUID(),
                data.isTransformed(),                     // 是否变身
                data.getState(),                          // 状态枚举
                session != null ? session.formId() : null, // 当前形态ID
                data.getPendingFormId()                   // 待处理形态ID
        );
        PacketDistributor.sendToPlayer(player, stateSyncPacket);
    }

    /**
     * 同步驱动器数据
     */
    public void syncDriverData(ServerPlayer player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        Map<ResourceLocation, ItemStack> mainItems = data.getMainDriverItems().getOrDefault(config.getRiderId(), new HashMap<>());
        Map<ResourceLocation, ItemStack> auxItems = data.getAuxDriverItems().getOrDefault(config.getRiderId(), new HashMap<>());

        PacketDistributor.sendToPlayer(player, new DriverDataSyncPacket(
                player.getUUID(),
                new HashMap<>(mainItems),
                new HashMap<>(auxItems)
        ));
    }
}
