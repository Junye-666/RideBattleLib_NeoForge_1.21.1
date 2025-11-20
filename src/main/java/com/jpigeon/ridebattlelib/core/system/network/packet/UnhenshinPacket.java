package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.network.handler.UUIDStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record UnhenshinPacket(UUID playerId) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "unhenshin");

    public static final Type<UnhenshinPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UnhenshinPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDStreamCodec.INSTANCE,
                    UnhenshinPacket::playerId,
                    UnhenshinPacket::new
            );

    @Override
    public @NotNull Type<?> type() { return TYPE; }
}
