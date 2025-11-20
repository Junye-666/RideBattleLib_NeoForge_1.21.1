package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.network.handler.UUIDStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record DriverDataSyncPacket(
        UUID playerId,
        Map<ResourceLocation, ItemStack> mainItems,
        Map<ResourceLocation, ItemStack> auxItems
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "driver_sync");

    public static final StreamCodec<RegistryFriendlyByteBuf, DriverDataSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDStreamCodec.INSTANCE,
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
    public @NotNull Type<?> type() { return TYPE; }
}
