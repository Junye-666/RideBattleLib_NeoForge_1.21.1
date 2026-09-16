package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class RotateSkillPacket implements RBLPacket {
    private RotateSkillPacket() {
    }

    public static final ResourceLocation ID = RBLPacket.ofPath("rotate_skill");

    public static final  RotateSkillPacket INSTANCE = new RotateSkillPacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, RotateSkillPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public static final Type<RotateSkillPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
