package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record InsertItemPacket(ResourceLocation slotId, ItemStack stack) implements RBLPacket {
    public static final ResourceLocation ID = RBLPacket.ofPath("insert_item");

    public static final StreamCodec<RegistryFriendlyByteBuf, InsertItemPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            InsertItemPacket::slotId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            InsertItemPacket::stack,
            InsertItemPacket::new
    );

    public static final Type<InsertItemPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
