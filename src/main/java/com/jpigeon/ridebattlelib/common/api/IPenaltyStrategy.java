package com.jpigeon.ridebattlelib.common.api;

import net.minecraft.world.entity.player.Player;

/**
 * 吃瘪系统
 */
public interface IPenaltyStrategy {
    boolean penaltyEnabled();

    float getPenaltyThreshold();

    long getCooldownDuration();

    float getExplosionPower();

    float getKnockBackStrength();

    float getResetHealth();

    boolean shouldTriggerPenalty(Player player);

    /**
     * 强制解除玩家变身
     */
    void performPenaltyUnhenshin(Player player);

    /**
     * 检查玩家是否处于冷却状态
     */
    boolean isInCooldown(Player player);
}
