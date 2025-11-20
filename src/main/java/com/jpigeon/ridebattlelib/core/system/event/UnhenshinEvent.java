package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 解除变身事件
 * <p>
 * 用于解除变身时播放音效等
 */
public class UnhenshinEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final ResourceLocation formId;
    private final boolean isPenalty;

    public UnhenshinEvent(Player player, ResourceLocation riderId, ResourceLocation formId, boolean isPenalty) {
        this.player = player;
        this.riderId = riderId;
        this.formId = formId;
        this.isPenalty = isPenalty;
    }

    public static class Pre extends UnhenshinEvent implements ICancellableEvent {
        public Pre(Player player, ResourceLocation riderId, ResourceLocation formId, boolean isPenalty) {
            super(player, riderId, formId, isPenalty);
        }
    }

    public static class Post extends UnhenshinEvent {
        public Post(Player player, ResourceLocation riderId, ResourceLocation formId, boolean isPenalty) {
            super(player, riderId, formId, isPenalty);
        }
    }

    public Player getPlayer() { return player; }
    public ResourceLocation getRiderId() { return riderId; }
    public ResourceLocation getFormId() { return formId; }
    public boolean isPenalty() { return isPenalty; }
}
