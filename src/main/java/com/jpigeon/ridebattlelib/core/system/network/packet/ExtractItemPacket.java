package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ExtractItemPacket(UUID playerId, ResourceLocation slotId
) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "extract_item");

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    ExtractItemPacket::playerId,
                    ResourceLocation.STREAM_CODEC,
                    ExtractItemPacket::slotId,
                    ExtractItemPacket::new
            );

    public static final Type<ExtractItemPacket> TYPE = new Type<>(ID);

    @Override
    public @NotNull Type<?> type() {
        return TYPE;
    }
}