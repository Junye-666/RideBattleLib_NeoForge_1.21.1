package com.jpigeon.ridebattlelib.core.system.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class HenshinEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final ResourceLocation formId;

    public HenshinEvent(Player player, ResourceLocation riderId, ResourceLocation formId) {
        this.player = player;
        this.riderId = riderId;
        this.formId = formId;
    }

    // Pre事件（可取消）
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

        public void setCanceled(boolean canceled) {
            if (!isCancelable()) {
                throw new UnsupportedOperationException("Attempted to cancel a non-cancelable event");
            }
            this.canceled = canceled;
        }
    }

    // Post事件（不可取消）
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
