package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 变身事件
 */
public class HenshinEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final ResourceLocation formId;

    public HenshinEvent(Player player, ResourceLocation riderId, ResourceLocation formId) {
        this.player = player;
        this.riderId = riderId;
        this.formId = formId;
    }

    public static class Pre extends HenshinEvent implements ICancellableEvent {
        public Pre(Player player, ResourceLocation riderId, ResourceLocation formId) {
            super(player, riderId, formId);
        }
    }

    public static class Post extends HenshinEvent {
        public Post(Player player, ResourceLocation riderId, ResourceLocation formId) {
            super(player, riderId, formId);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getRiderId() {
        return riderId;
    }

    public ResourceLocation getFormId() {
        return formId;
    }
}
