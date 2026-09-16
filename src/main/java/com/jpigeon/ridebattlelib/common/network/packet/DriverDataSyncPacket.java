package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record DriverDataSyncPacket(
        UUID playerId,
        Map<ResourceLocation, ItemStack> mainItems,
        Map<ResourceLocation, ItemStack> auxItems
) implements RBLPacket {

    public static final ResourceLocation ID = RBLPacket.ofPath("driver_sync");

    public static final StreamCodec<RegistryFriendlyByteBuf, DriverDataSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    DriverDataSyncPacket::playerId,
                    createMapCodec(),
                    DriverDataSyncPacket::mainItems,
                    createMapCodec(),
                    DriverDataSyncPacket::auxItems,
                    DriverDataSyncPacket::new
            );

    private static StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, ItemStack>> createMapCodec() {
        return StreamCodec.of(
                (buf, map) -> {
                    buf.writeVarInt(map.size());
                    for (Map.Entry<ResourceLocation, ItemStack> entry : map.entrySet()) {
                        ResourceLocation.STREAM_CODEC.encode(buf, entry.getKey());
                        ItemStack.STREAM_CODEC.encode(buf, entry.getValue());
                    }
                },
                buf -> {
                    Map<ResourceLocation, ItemStack> map = new HashMap<>();
                    int size = buf.readVarInt();
                    for (int i = 0; i < size; i++) {
                        ResourceLocation key = ResourceLocation.STREAM_CODEC.decode(buf);
                        ItemStack value = ItemStack.STREAM_CODEC.decode(buf);
                        map.put(key, value);
                    }
                    return map;
                }
        );
    }

    public static final Type<DriverDataSyncPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
