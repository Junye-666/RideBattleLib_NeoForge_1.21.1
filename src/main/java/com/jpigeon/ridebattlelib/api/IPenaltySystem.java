package com.jpigeon.ridebattlelib.api;

import net.minecraft.world.entity.player.Player;

/**
 * 吃瘪系统
 */
public interface IPenaltySystem {
    /**
     * 强制解除玩家变身
     */
    void penaltyUnhenshin(Player player);
    /**
     * 开始变身冷却
     */
    void startCooldown(Player player, int seconds);
    /**
     * 检查玩家是否处于冷却状态
     */
    boolean isInCooldown(Player player);
}
