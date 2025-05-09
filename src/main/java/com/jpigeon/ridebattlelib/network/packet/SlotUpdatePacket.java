package com.jpigeon.ridebattlelib.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record SlotUpdatePacket(int slot, ItemStack stack) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SlotUpdatePacket::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, // 使用新版 ItemStack 编解码
            SlotUpdatePacket::stack,
            SlotUpdatePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
