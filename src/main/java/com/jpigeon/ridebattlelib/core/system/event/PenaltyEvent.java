package com.jpigeon.ridebattlelib.core.system.event;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class PenaltyEvent extends Event {
    private final Player player;

    public PenaltyEvent(Player player) {
        this.player = player;
    }

    public static class Sound extends PenaltyEvent {
        private boolean canceled = false;

        public Sound(Player player) {
            super(player);
        }

        public boolean isCancelable() {
            return true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class Explosion extends PenaltyEvent {
        private boolean canceled = false;

        public Explosion(Player player) {
            super(player);
        }
        public boolean isCancelable() {
            return true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class Particle extends PenaltyEvent {
        private boolean canceled = false;

        public Particle(Player player) {
            super(player);
        }
        public boolean isCancelable() {
            return true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public Player getPlayer() {
        return player;
    }
}
