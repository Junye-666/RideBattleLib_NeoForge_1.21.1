package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.data.TransformedData;
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

    public UnhenshinEvent(Player player, TransformedData data) {
        this.player = player;
        this.riderId = data.config().getRiderId();
        this.formId = data.formId();
        this.isPenalty = player.getHealth() == Config.PENALTY_THRESHOLD.get();
    }

    public static class Pre extends UnhenshinEvent implements ICancellableEvent {
        public Pre(Player player, TransformedData data) {
            super(player, data);
        }
    }

    public static class Post extends UnhenshinEvent {
        public Post(Player player, TransformedData data) {
            super(player, data);
        }
    }

    public Player getPlayer() { return player; }
    public ResourceLocation getRiderId() { return riderId; }
    public ResourceLocation getFormId() { return formId; }
    public boolean isPenalty() { return isPenalty; }
}
