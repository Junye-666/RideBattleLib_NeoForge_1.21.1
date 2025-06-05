package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class AnimationEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final AnimationPhase phase;
    private boolean canceled = false;

    public AnimationEvent(Player player, ResourceLocation riderId, AnimationPhase phase) {
        this.player = player;
        this.riderId = riderId;
        this.phase = phase;
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getRiderId() {
        return riderId;
    }

    public AnimationPhase getPhase() {
        return phase;
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