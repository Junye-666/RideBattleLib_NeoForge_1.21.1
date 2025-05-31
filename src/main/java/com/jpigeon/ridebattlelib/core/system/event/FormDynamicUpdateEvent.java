package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class FormDynamicUpdateEvent extends Event {
    private final Player player;
    private final ResourceLocation formId;

    public FormDynamicUpdateEvent(Player player, ResourceLocation formId) {
        this.player = player;
        this.formId = formId;
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getFormId() {
        return formId;
    }
}