package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.network.handler.UUIDStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record BeltDataDiffPacket(
        UUID playerId,
        Map<ResourceLocation, ItemStack> changes,
        boolean fullSync
) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "belt_diff_sync");

    public static final Type<BeltDataDiffPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BeltDataDiffPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDStreamCodec.INSTANCE,
                    BeltDataDiffPacket::playerId,
                    createChangesCodec(),
                    BeltDataDiffPacket::changes,
                    ByteBufCodecs.BOOL,
                    BeltDataDiffPacket::fullSync,
                    BeltDataDiffPacket::new
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
    public @NotNull Type<?> type() { return TYPE; }
}
