package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record HenshinPacket(ResourceLocation riderId) implements RBLPacket {
    public static final ResourceLocation ID = RBLPacket.ofPath("henshin");

    public static final StreamCodec<RegistryFriendlyByteBuf, HenshinPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    HenshinPacket::riderId,
                    HenshinPacket::new
            );

    public static final Type<HenshinPacket> TYPE = new Type<>(ID);


    @Override
    public ResourceLocation id() {
        return ID;
    }
}
