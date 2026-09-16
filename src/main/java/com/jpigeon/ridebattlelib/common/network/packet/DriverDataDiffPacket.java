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

public record DriverDataDiffPacket(
        UUID playerId,
        Map<ResourceLocation, ItemStack> changes
) implements RBLPacket {

    public static final ResourceLocation ID = RBLPacket.ofPath("driver_diff_sync");

    public static final Type<DriverDataDiffPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, DriverDataDiffPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    DriverDataDiffPacket::playerId,
                    createChangesCodec(),
                    DriverDataDiffPacket::changes,
                    DriverDataDiffPacket::new
            );

    private static StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, ItemStack>> createChangesCodec() {
        return StreamCodec.of(
                (buf, changes) -> {
                    buf.writeVarInt(changes.size());
                    for (Map.Entry<ResourceLocation, ItemStack> entry : changes.entrySet()) {
                        ResourceLocation.STREAM_CODEC.encode(buf, entry.getKey());

                        if (entry.getValue().isEmpty()) {
                            buf.writeBoolean(false);
                        } else {
                            buf.writeBoolean(true);
                            ItemStack.STREAM_CODEC.encode(buf, entry.getValue());
                        }
                    }
                },
                buf -> {
                    Map<ResourceLocation, ItemStack> changes = new HashMap<>();
                    int size = buf.readVarInt();
                    for (int i = 0; i < size; i++) {
                        ResourceLocation slotId = ResourceLocation.STREAM_CODEC.decode(buf);
                        boolean hasItem = buf.readBoolean();

                        if (hasItem) {
                            ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                            changes.put(slotId, stack);
                        } else {
                            changes.put(slotId, ItemStack.EMPTY);
                        }
                    }
                    return changes;
                }
        );
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
