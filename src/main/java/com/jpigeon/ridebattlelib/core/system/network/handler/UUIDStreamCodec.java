package com.jpigeon.ridebattlelib.core.system.network.handler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public enum UUIDStreamCodec implements StreamCodec<RegistryFriendlyByteBuf, UUID> {
    INSTANCE;

    @Override
    public @NotNull UUID decode(RegistryFriendlyByteBuf buf) {
        return buf.readUUID();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf, UUID value) {
        buf.writeUUID(value);
    }
}
