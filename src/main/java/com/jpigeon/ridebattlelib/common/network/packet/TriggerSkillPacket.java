package com.jpigeon.ridebattlelib.common.network.packet;

import com.jpigeon.ridebattlelib.common.network.RBLPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class TriggerSkillPacket implements RBLPacket {
    private TriggerSkillPacket() {
    }

    public static final ResourceLocation ID = RBLPacket.ofPath("trigger_skill");

    public static final TriggerSkillPacket INSTANCE = new TriggerSkillPacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerSkillPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public static final Type<TriggerSkillPacket> TYPE = new Type<>(ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
