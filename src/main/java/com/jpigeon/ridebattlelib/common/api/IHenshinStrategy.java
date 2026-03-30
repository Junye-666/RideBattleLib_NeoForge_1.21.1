package com.jpigeon.ridebattlelib.common.api;

import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 变身助手接口 - 简化版本
 */
public interface IHenshinStrategy {
    /**
     * 执行变身，包含设置变身数据
     */
    void performHenshin(Player player, RiderConfig config, ResourceLocation formId);

    /**
     * 执行形态切换
     */
    void performFormSwitch(Player player, HenshinSessionData data, ResourceLocation newFormId);

    /**
     * 解除变身方法
     */
    void unHenshin(Player player, HenshinSessionData data);
}
