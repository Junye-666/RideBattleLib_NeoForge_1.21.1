package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinStateSyncPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.TransformedStatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SyncManager {
    public static final SyncManager INSTANCE = new SyncManager();

    /**
     * 同步玩家所有相关状态
     */
    public void syncAllPlayerData(ServerPlayer player) {
        syncHenshinState(player);
        syncDriverData(player);
        syncTransformedState(player);
    }

    /**
     * 同步变身状态
     */
    public void syncHenshinState(ServerPlayer player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("同步变身状态: player={}, state={}, pendingForm={}",
                    player.getName().getString(), data.getHenshinState(), data.getPendingFormId());
        }

        PacketHandler.sendToClient(player, new HenshinStateSyncPacket(
                player.getUUID(),
                data.getHenshinState(),
                data.getPendingFormId()
        ));
    }

    /**
     * 同步驱动器数据
     */
    public void syncDriverData(ServerPlayer player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        Map<ResourceLocation, ItemStack> mainItems = data.getDriverItems(config.getRiderId());
        Map<ResourceLocation, ItemStack> auxItems = data.auxDriverItems.getOrDefault(config.getRiderId(), new HashMap<>());

        PacketHandler.sendToClient(player, new DriverDataSyncPacket(
                player.getUUID(),
                new HashMap<>(mainItems),
                new HashMap<>(auxItems)
        ));
    }

    /**
     * 同步变身状态
     */
    public void syncTransformedState(ServerPlayer player) {
        boolean isTransformed = HenshinSystem.INSTANCE.isTransformed(player);
        PacketHandler.sendToClient(player, new TransformedStatePacket(player.getUUID(), isTransformed));
    }
}
