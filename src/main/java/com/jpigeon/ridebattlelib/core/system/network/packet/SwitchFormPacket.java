package com.jpigeon.ridebattlelib.core.system.network.packet;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


public record SwitchFormPacket(ResourceLocation formId) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "switch_form");

    public static final Type<SwitchFormPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SwitchFormPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.map(
                    rl -> rl == null ? ResourceLocation.withDefaultNamespace("null") : rl,
                    rl -> rl.toString().equals("null") ? null : rl
            ),
            SwitchFormPacket::formId,
            SwitchFormPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
