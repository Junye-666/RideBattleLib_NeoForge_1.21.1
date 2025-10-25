package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.Collections;
import java.util.Map;

/**
 * 在变身前匹配形态时强制修改匹配形态的覆盖(相当于形态琐)
 */
public class FormOverrideEvent extends Event {
    private final Player player;
    private final Map<ResourceLocation, ItemStack> driverItems;
    private final ResourceLocation currentForm;
    private ResourceLocation overrideForm;
    private boolean canceled = false;

    public FormOverrideEvent(Player player, Map<ResourceLocation, ItemStack> driverItems, ResourceLocation currentForm) {
        this.player = player;
        this.driverItems = Collections.unmodifiableMap(driverItems);
        this.currentForm = currentForm;
        this.overrideForm = null;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * 强制覆盖形态
     * @param overrideForm 变身时强制更改至的形态
     */
    public void setOverrideForm(ResourceLocation overrideForm) {
        this.overrideForm = overrideForm;
    }

    public Map<ResourceLocation, ItemStack> getDriverItems() {
        return driverItems;
    }

    public ResourceLocation getCurrentForm() {
        return currentForm;
    }

    public ResourceLocation getOverrideForm() {
        return overrideForm;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}