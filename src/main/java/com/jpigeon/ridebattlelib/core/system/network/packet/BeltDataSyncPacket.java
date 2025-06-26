package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record BeltDataSyncPacket(UUID playerId, Map<ResourceLocation, ItemStack> items) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "belt_sync");

    public static final StreamCodec<RegistryFriendlyByteBuf, BeltDataSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    uuidCodec(),
                    BeltDataSyncPacket::playerId,
                    mapCodec(),
                    BeltDataSyncPacket::items,
                    BeltDataSyncPacket::new
            );

    private static StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, ItemStack>> mapCodec() {
        return StreamCodec.of(
                (buf, map) -> {
                    // 过滤空堆栈
                    Map<ResourceLocation, ItemStack> filtered = map.entrySet().stream()
                            .filter(entry -> !entry.getValue().isEmpty())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    buf.writeVarInt(filtered.size());
                    filtered.forEach((key, value) -> {
                        ResourceLocation.STREAM_CODEC.encode(buf, key);
                        ItemStack.STREAM_CODEC.encode(buf, value.copy());
                    });
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

    private static StreamCodec<RegistryFriendlyByteBuf, UUID> uuidCodec() {
        return StreamCodec.of(
                (buf, uuid) -> buf.writeUUID(uuid),
                buf -> buf.readUUID()
        );
    }

    public static final Type<BeltDataSyncPacket> TYPE = new Type<>(ID);

    @Override
    public @NotNull Type<?> type() { return TYPE; }
}
