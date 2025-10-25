package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class HenshinPauseEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final ResourceLocation formId;

    public HenshinPauseEvent(Player player, ResourceLocation riderId, ResourceLocation formId) {
        this.player = player;
        this.riderId = riderId;
        this.formId = formId;
    }

    public static class Pre extends HenshinEvent {
        private boolean canceled = false;

        public Pre(Player player, ResourceLocation riderId, ResourceLocation formId) {
            super(player, riderId, formId);
        }

        public boolean isCancelable() {
            return true;
        }

        public boolean isCanceled() {
            return canceled;
        }
        /**
         * 取消暂停以直接进行变身
         */
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
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
