package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import com.jpigeon.ridebattlelib.common.util.PayloadUtils;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record HenshinStateSyncPacket(
        UUID playerId,
        boolean isTransformed,
        HenshinState state,
        ResourceLocation riderId,
        ResourceLocation currentFormId,
        ResourceLocation pendingFormId
) implements RBLPacket {

    public static final ResourceLocation ID = RBLPacket.ofPath("henshin_state_sync");

    public static final StreamCodec<RegistryFriendlyByteBuf, HenshinStateSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, HenshinStateSyncPacket::playerId,
                    ByteBufCodecs.BOOL, HenshinStateSyncPacket::isTransformed,
                    StreamCodec.of(
                            (buf, s) -> buf.writeByte(s.ordinal()),
                            buf -> HenshinState.values()[buf.readByte()]
                    ), HenshinStateSyncPacket::state,
                    PayloadUtils.nullableResourceLocation(), HenshinStateSyncPacket::riderId,
                    PayloadUtils.nullableResourceLocation(), HenshinStateSyncPacket::currentFormId,
                    PayloadUtils.nullableResourceLocation(), HenshinStateSyncPacket::pendingFormId,
                    HenshinStateSyncPacket::new
            );

    public static final Type<HenshinStateSyncPacket> TYPE = new Type<>(ID);


    @Override
    public ResourceLocation id() {
        return ID;
    }
}