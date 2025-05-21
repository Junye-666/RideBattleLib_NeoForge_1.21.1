package com.jpigeon.ridebattlelib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IAttributeSystem {
    // 属性管理
    void applyAttributes(Player player, ResourceLocation formId);
    void removeAttributes(Player player);

    // 效果管理
    void applyEffects(Player player, ResourceLocation formId);
    void removeEffects(Player player);
}
