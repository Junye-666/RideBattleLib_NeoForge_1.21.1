package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 解除变身事件
 * <p>
 * 用于解除变身时播放音效等
 */
public class UnhenshinEvent extends Event {
    private final Player player;

    public UnhenshinEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public static class Pre extends UnhenshinEvent implements ICancellableEvent {
        public Pre(Player player) {
            super(player);
        }
    }

    public static class Post extends UnhenshinEvent {
        public Post(Player player) {
            super(player);
        }
    }
}
