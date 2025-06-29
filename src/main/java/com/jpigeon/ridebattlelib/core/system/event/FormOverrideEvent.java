package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.Collections;
import java.util.Map;

// 在变身前匹配形态时强制覆盖匹配的形态(相当于形态琐)
public class FormOverrideEvent extends Event {
    private final Player player;
    private final Map<ResourceLocation, ItemStack> beltItems;
    private final ResourceLocation currentForm;
    private ResourceLocation overrideForm;
    private boolean canceled = false;

    public FormOverrideEvent(Player player, Map<ResourceLocation, ItemStack> beltItems, ResourceLocation currentForm) {
        this.player = player;
        this.beltItems = Collections.unmodifiableMap(beltItems);
        this.currentForm = currentForm;
        this.overrideForm = null;
    }

    public Player getPlayer() {
        return player;
    }

    public Map<ResourceLocation, ItemStack> getBeltItems() {
        return beltItems;
    }

    public ResourceLocation getCurrentForm() {
        return currentForm;
    }

    public ResourceLocation getOverrideForm() {
        return overrideForm;
    }

    public void setOverrideForm(ResourceLocation overrideForm) {
        this.overrideForm = overrideForm;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}