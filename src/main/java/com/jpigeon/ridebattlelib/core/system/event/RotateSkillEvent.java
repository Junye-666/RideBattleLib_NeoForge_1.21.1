package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RotateSkillEvent extends Event implements ICancellableEvent {
    private final Player player;

    public RotateSkillEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

}
