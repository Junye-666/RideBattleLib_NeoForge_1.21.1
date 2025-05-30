package com.jpigeon.ridebattlelib.core.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record HenshinPacket(ResourceLocation riderId) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "henshin");

    public static final StreamCodec<RegistryFriendlyByteBuf, HenshinPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    HenshinPacket::riderId,
                    HenshinPacket::new
            );

    public static final Type<HenshinPacket> TYPE = new Type<>(ID);

    @Override
    public @NotNull Type<?> type() { return TYPE; }

}
