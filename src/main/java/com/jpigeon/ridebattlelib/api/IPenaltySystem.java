package com.jpigeon.ridebattlelib.api;

import net.minecraft.world.entity.player.Player;

public interface IPenaltySystem {
    void forceUnhenshin(Player player);
    void startCooldown(Player player, int seconds);
    boolean isInCooldown(Player player);
}
