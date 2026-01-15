package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 允许在搜索RiderConfig时插入其他逻辑
 */
public class FindRiderConfigEvent extends Event implements ICancellableEvent {
    private final Player player;
    private RiderConfig config;

    public FindRiderConfigEvent(Player player) {
        this.player = player;
        this.config = null;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * 强制匹配骑士Config
     * @param config 返回时强制匹配
     */
    public void setConfig(RiderConfig config) {
        this.config = config;
    }

    public RiderConfig getConfig() {
        return config;
    }
}
