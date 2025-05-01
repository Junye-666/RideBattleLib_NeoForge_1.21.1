package com.jpigeon.ridebattlelib.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record UnhenshinPacket() implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "unhenshin");

    public static final CustomPacketPayload.Type<UnhenshinPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UnhenshinPacket> STREAM_CODEC =
            StreamCodec.unit(new UnhenshinPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;  //返回预定义的TYPE常量
    }
}
