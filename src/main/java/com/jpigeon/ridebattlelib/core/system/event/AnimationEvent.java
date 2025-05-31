package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class AnimationEvent extends Event {
    private final Player player;
    private final ResourceLocation formId;
    private final AnimationPhase phase;

    public AnimationEvent(Player player, ResourceLocation formId, AnimationPhase phase) {
        this.player = player;
        this.formId = formId;
        this.phase = phase;
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getFormId() {
        return formId;
    }

    public AnimationPhase getPhase() {
        return phase;
    }
}
