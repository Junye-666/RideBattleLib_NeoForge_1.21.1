package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.jpigeon.ridebattlelib.core.system.network.handler.UUIDStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public record HenshinStateSyncPacket(UUID playerId, HenshinState state,
                                     @Nullable ResourceLocation pendingFormId)
        implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "henshin_state_sync");

    public static final Type<HenshinStateSyncPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, HenshinStateSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDStreamCodec.INSTANCE,
                    HenshinStateSyncPacket::playerId,
                    ByteBufCodecs.fromCodec(HenshinState.CODEC),
                    HenshinStateSyncPacket::state,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC)
                            .map(opt -> opt.orElse(null),
                                    Optional::ofNullable),
                    HenshinStateSyncPacket::pendingFormId,
                    HenshinStateSyncPacket::new
            );

    @Override
    public @NotNull Type<?> type() { return TYPE; }
}