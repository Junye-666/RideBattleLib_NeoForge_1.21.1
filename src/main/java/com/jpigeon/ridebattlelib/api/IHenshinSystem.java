package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * 故事从此开始!
 * 假面骑士的变身系统
 */
public interface IHenshinSystem {
    /**
     * 玩家触发驱动器
     */
    void driverAction(Player player);
    /**
     * 变身
     * @param player 玩家
     * @param riderId 骑士ID
     * @return 是否成功
     */
    boolean henshin(Player player, ResourceLocation riderId);
    /**
     * 解除玩家变身
     */
    void unHenshin(Player player);
    /**
     * 切换形态
     * @param newFormId 切换至的形态ID
     */
    void switchForm(Player player, ResourceLocation newFormId);
    /**
     * @return 玩家是否处于变身状态
     */
    boolean isTransformed(Player player);
    /**
     * 获取玩家变身数
     */
    @Nullable HenshinSystem.TransformedData getTransformedData(Player player);
}
