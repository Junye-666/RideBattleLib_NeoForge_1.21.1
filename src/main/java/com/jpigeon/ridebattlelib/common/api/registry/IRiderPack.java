package com.jpigeon.ridebattlelib.common.api.registry;

import net.minecraft.resources.ResourceLocation;

public interface IRiderPack {
    ResourceLocation riderId();

    default void registerCommon() {
    }

    default void registerClient() {
    }
}
