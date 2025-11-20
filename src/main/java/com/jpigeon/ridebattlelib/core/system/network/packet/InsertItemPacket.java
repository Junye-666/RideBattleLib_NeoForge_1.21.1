package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.network.handler.UUIDStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record InsertItemPacket(UUID playerId, ResourceLocation slotId, ItemStack stack) implements CustomPacketPayload {
    public static final Type<InsertItemPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "insert_item"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, InsertItemPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDStreamCodec.INSTANCE,
            InsertItemPacket::playerId,
            ResourceLocation.STREAM_CODEC,
            InsertItemPacket::slotId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            InsertItemPacket::stack,
            InsertItemPacket::new
    );
}
