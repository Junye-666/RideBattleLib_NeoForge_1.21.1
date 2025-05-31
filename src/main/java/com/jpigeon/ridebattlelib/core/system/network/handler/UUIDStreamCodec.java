package com.jpigeon.ridebattlelib.core.system.network.handler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public enum UUIDStreamCodec implements StreamCodec<RegistryFriendlyByteBuf, UUID> {
    INSTANCE;

    @Override
    public UUID decode(RegistryFriendlyByteBuf buf) {
        return buf.readUUID();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf, UUID value) {
        buf.writeUUID(value);
    }
}
