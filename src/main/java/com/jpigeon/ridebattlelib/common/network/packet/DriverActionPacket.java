package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class DriverActionPacket implements RBLPacket {
    private DriverActionPacket() {
    }

    public static final ResourceLocation ID = RBLPacket.ofPath("driver_action");

    public static final DriverActionPacket INSTANCE = new DriverActionPacket();


    public static final StreamCodec<RegistryFriendlyByteBuf, DriverActionPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public static final Type<DriverActionPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
