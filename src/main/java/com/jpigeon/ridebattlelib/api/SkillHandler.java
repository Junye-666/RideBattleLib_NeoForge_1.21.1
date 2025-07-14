package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface SkillHandler {
    void execute(Player player, ResourceLocation skillId);
}
