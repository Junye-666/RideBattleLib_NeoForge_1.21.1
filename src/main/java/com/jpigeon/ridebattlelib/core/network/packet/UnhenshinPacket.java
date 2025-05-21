package com.jpigeon.ridebattlelib.core.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record UnhenshinPacket() implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "unhenshin");

    public static final Type<UnhenshinPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UnhenshinPacket> STREAM_CODEC =
            StreamCodec.unit(new UnhenshinPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
