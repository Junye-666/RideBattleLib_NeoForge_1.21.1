package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class ItemGrantEvent extends Event {
    private final Player player;
    private ItemStack stack;
    private final FormConfig config;

    public ItemGrantEvent(Player player, ItemStack stack, FormConfig config) {
        this.player = player;
        this.stack = stack;
        this.config = config;
    }

    public static class Pre extends ItemGrantEvent {
        private boolean canceled = false;

        public Pre(Player player, ItemStack stack, FormConfig config) {
            super(player, stack, config);
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class Post extends ItemGrantEvent {
        public Post(Player player, ItemStack stack, FormConfig config) {
            super(player, stack, config);
        }
    }

    /**
     * 修改玩家获得的物品
     */
    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Player getPlayer() {
        return player;
    }

    public FormConfig getConfig() {
        return config;
    }
}
