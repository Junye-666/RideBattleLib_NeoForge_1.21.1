package com.jpigeon.ridebattlelib.api;

import net.minecraft.world.entity.player.Player;

public interface IPenaltySystem {
    // 强制解除
    void forceUnhenshin(Player player);
    // 冷却
    void startCooldown(Player player, int seconds);
    // 检查
    boolean isInCooldown(Player player);
}
