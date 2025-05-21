package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IHenshinSystem {
    // 核心方法
    boolean henshin(Player player, ResourceLocation riderId);
    void unHenshin(Player player);
    // 检查是否已变身
    boolean isTransformed(Player player);
    // 接口
    default void onHenshinStart(Player player){}
    default void onHenshinEnd(Player player){}
    // 检查驱动器
    default boolean validateDriver(Player player, RiderConfig config) {
        return player != null && config != null &&
                player.getItemBySlot(config.getDriverSlot()).is(config.getDriverItem());
    }
    // 玩家是否可变身
    default boolean canTransform(Player player, RiderConfig config) {
        return validateDriver(player, config) && !isTransformed(player);
    }
}
