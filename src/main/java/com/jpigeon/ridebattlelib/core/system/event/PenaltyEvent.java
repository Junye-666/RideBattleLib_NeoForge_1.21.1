package com.jpigeon.ridebattlelib.core.system.event;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 吃瘪事件
 */
public class PenaltyEvent extends Event {
    private final Player player;

    public PenaltyEvent(Player player) {
        this.player = player;
    }

    public static class Sound extends PenaltyEvent implements ICancellableEvent {
        public Sound(Player player) {
            super(player);
        }
    }

    public static class Explosion extends PenaltyEvent implements ICancellableEvent{
        public Explosion(Player player) {
            super(player);
        }
    }

    public static class Particle extends PenaltyEvent implements ICancellableEvent{
        public Particle(Player player) {
            super(player);
        }
    }

    public Player getPlayer() {
        return player;
    }
}
