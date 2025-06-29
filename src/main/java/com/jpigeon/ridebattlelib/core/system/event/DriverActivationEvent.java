package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class DriverActivationEvent extends Event {
    private final Player player;
    private final ItemStack driverItem;
    private boolean activated = true;

    public DriverActivationEvent(Player player, ItemStack driverItem) {
        this.player = player;
        this.driverItem = driverItem;
    }

    public boolean isCanceled() {
        return activated;
    }

    public void setCanceled(boolean canceled) {
        this.activated = canceled;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getDriverItem() {
        return driverItem;
    }

    public boolean isActivated() {
        return activated;
    }
}
