package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class ReturnItemsEvent extends Event {
    private final Player player;
    private final RiderConfig config;
    private boolean canceled = false;

    public ReturnItemsEvent(Player player, RiderConfig config) {
        this.player = player;
        this.config = config;
    }

    public Player getPlayer() {
        return player;
    }

    public RiderConfig getConfig() {
        return config;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public static class Pre extends ReturnItemsEvent {
        public Pre(Player player, RiderConfig config) {
            super(player, config);
        }
    }

    public static class Post extends ReturnItemsEvent {
        public Post(Player player, RiderConfig config) {
            super(player, config);
        }
    }
}