package com.jpigeon.ridebattlelib.common.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum HenshinState implements StringRepresentable {
    IDLE("idle"),
    TRANSFORMING("transforming"),
    TRANSFORMED("transformed");

    private final String name;

    HenshinState(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public static final Codec<HenshinState> CODEC = StringRepresentable.fromEnum(HenshinState::values);
}
