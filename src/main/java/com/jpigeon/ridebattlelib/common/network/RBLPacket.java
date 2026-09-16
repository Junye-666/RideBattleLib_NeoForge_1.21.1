package com.jpigeon.ridebattlelib.common.network;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public interface RBLPacket extends CustomPacketPayload {
    ResourceLocation id();

    @Override
    default Type<? extends CustomPacketPayload> type() {
        return new Type<>(id());
    }

    static ResourceLocation ofPath(String path) {
        return ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, path);
    }
}
