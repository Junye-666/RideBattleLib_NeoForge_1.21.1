package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncHenshinStatePacket(
        HenshinState state,
        ResourceLocation pendingFormId
) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "sync_henshin_state");

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHenshinStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(HenshinState.CODEC),
                    SyncHenshinStatePacket::state,
                    ResourceLocation.STREAM_CODEC,
                    SyncHenshinStatePacket::pendingFormId,
                    SyncHenshinStatePacket::new
            );

    public static final Type<SyncHenshinStatePacket> TYPE = new Type<>(ID);

    @Override
    public @NotNull Type<?> type() { return TYPE; }
}
