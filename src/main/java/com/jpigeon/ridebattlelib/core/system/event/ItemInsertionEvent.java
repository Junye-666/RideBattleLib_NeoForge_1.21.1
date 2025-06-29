package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class ItemInsertionEvent extends Event {
    private final Player player;
    private final ResourceLocation slotId;
    private ItemStack stack;
    private final RiderConfig config;

    public ItemInsertionEvent(Player player, ResourceLocation slotId, ItemStack stack, RiderConfig config) {
        this.player = player;
        this.slotId = slotId;
        this.stack = stack;
        this.config = config;
    }

    public static class Pre extends ItemInsertionEvent {
        private boolean canceled = false;

        public Pre(Player player, ResourceLocation slotId, ItemStack stack, RiderConfig config) {
            super(player, slotId, stack, config);
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class Post extends ItemInsertionEvent {
        public Post(Player player, ResourceLocation slotId, ItemStack stack, RiderConfig config) {
            super(player, slotId, stack, config);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getSlotId() {
        return slotId;
    }

    public ItemStack getStack() {
        return stack;
    }

    // 可以通过这个强制修改玩家插入的物品(嘿嘿)
    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public RiderConfig getConfig() {
        return config;
    }
}
