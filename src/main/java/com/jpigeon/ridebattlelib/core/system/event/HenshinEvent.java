package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.LogicalSide;

public class HenshinEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final ResourceLocation formId;
    private final LogicalSide side;

    public HenshinEvent(Player player, ResourceLocation riderId, ResourceLocation formId, LogicalSide side) {
        this.player = player;
        this.riderId = riderId;
        this.formId = formId;
        this.side = player.level().isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }

    public static class Pre extends HenshinEvent {
        private boolean canceled = false;

        public Pre(Player player, ResourceLocation riderId, ResourceLocation formId, LogicalSide side) {
            super(player, riderId, formId, side);
        }

        public boolean isCancelable() {
            return true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class Post extends HenshinEvent {
        public Post(Player player, ResourceLocation riderId, ResourceLocation formId, LogicalSide side) {
            super(player, riderId, formId, side);
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

    public LogicalSide getSide() {
        return side;
    }
}
