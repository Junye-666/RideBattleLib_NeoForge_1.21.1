package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class CompleteHenshinPacket implements RBLPacket {
    private CompleteHenshinPacket() {
    }

    public static final ResourceLocation ID = RBLPacket.ofPath("complete_henshin");

    public static final CompleteHenshinPacket INSTANCE = new CompleteHenshinPacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, CompleteHenshinPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public static final Type<CompleteHenshinPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
