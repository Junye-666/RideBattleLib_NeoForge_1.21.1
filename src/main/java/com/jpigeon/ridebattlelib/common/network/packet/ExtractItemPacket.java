package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ExtractItemPacket(ResourceLocation slotId) implements RBLPacket {
    public static final ResourceLocation ID = RBLPacket.ofPath("extract_item");

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    ExtractItemPacket::slotId,
                    ExtractItemPacket::new
            );

    public static final Type<ExtractItemPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}