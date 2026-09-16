package com.jpigeon.ridebattlelib.common.api.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;

public record ClientRiderContext(
        @NotNull LocalPlayer player,
        @NotNull ItemStack driverStack,
        @Nullable ResourceLocation riderId,
        @Nullable ResourceLocation currentFormId,
        @Nullable ResourceLocation pendingFormId,
        @Nullable Map<ResourceLocation, ItemStack> changedItems,
        @Nullable ResourceLocation skillId,
        @Nullable ChangeType changeType
) {
    public enum ChangeType {HENSHIN, SWITCH, UNHENSHIN, PENDING, DRIVER_CHANGE, SKILL}
}
