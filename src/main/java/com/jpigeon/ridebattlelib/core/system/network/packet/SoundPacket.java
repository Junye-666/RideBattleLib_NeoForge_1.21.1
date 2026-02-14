package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SoundPacket(UUID playerId, ResourceLocation soundId, float volume, float pitch) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "play_sound");


    public static final StreamCodec<RegistryFriendlyByteBuf, SoundPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SoundPacket::playerId,
                    ResourceLocation.STREAM_CODEC, SoundPacket::soundId,
                    ByteBufCodecs.FLOAT, SoundPacket::volume,
                    ByteBufCodecs.FLOAT, SoundPacket::pitch,
                    SoundPacket::new
            );


    public static final CustomPacketPayload.Type<SoundPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    @Override
    public @NotNull CustomPacketPayload.Type<?> type() { return TYPE; }
}