package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IAnimationSystem {
    // 播放变身序列
    void playHenshinSequence(Player player, ResourceLocation formId, AnimationPhase phase);
}
