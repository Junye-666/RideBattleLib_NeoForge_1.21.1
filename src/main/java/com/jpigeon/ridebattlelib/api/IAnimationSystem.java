package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IAnimationSystem {
    // 动画播放
    void playHenshin(Player player, ResourceLocation formId);
    void playUnhenshin(Player player, ResourceLocation riderId);
}
