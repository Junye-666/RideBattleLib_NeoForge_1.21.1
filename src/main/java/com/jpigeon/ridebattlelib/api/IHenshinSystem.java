package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public interface IHenshinSystem {
    void driverAction(Player player);
    boolean henshin(Player player, ResourceLocation riderId);
    void unHenshin(Player player);
    void switchForm(Player player, ResourceLocation newFormId);
    boolean isTransformed(Player player);
    @Nullable HenshinSystem.TransformedData getTransformedData(Player player);
}
