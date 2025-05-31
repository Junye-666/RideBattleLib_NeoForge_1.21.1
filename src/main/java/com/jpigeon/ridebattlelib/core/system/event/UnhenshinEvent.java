package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class UnhenshinEvent extends Event {
    private final Player player;

    public UnhenshinEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    // Pre事件（可取消）
    public static class Pre extends UnhenshinEvent {
        private boolean canceled = false;

        public Pre(Player player) {
            super(player);
        }

        public boolean isCancelable() {
            return true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            if (!isCancelable()) {
                throw new UnsupportedOperationException("Attempted to cancel a non-cancelable event");
            }
            this.canceled = canceled;
        }
    }

    // Post事件（不可取消）
    public static class Post extends UnhenshinEvent {
        public Post(Player player) {
            super(player);
        }
    }
}
