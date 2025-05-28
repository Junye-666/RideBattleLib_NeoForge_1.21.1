package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IAnimationSystem {
    // 动画播放
    void playHenshin(Player player, ResourceLocation formId);
    void playUnhenshin(Player player, ResourceLocation riderId);
    void playHenshinSequence(Player player, ResourceLocation formId, AnimationPhase phase);
}
