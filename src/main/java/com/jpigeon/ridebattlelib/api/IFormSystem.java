package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IFormSystem {
    // 形态匹配
    ResourceLocation matchForm(Player player, ResourceLocation formId);
    // 切换形态
    void switchForm(Player player, ResourceLocation formId);
}
