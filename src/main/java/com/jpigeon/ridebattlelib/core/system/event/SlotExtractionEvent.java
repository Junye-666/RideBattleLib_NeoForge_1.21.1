package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class SlotExtractionEvent extends Event {
    private final Player player;
    private final ResourceLocation slotId;
    private ItemStack extractedStack;
    private final RiderConfig config;

    public SlotExtractionEvent(Player player, ResourceLocation slotId, ItemStack extractedStack, RiderConfig config) {
        this.player = player;
        this.slotId = slotId;
        this.extractedStack = extractedStack;
        this.config = config;
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getSlotId() {
        return slotId;
    }

    public ItemStack getExtractedStack() {
        return extractedStack;
    }

    public void setExtractedStack(ItemStack extractedStack) {
        this.extractedStack = extractedStack;
    }

    public RiderConfig getConfig() {
        return config;
    }

    public static class Pre extends SlotExtractionEvent {
        private boolean canceled = false;

        public Pre(Player player, ResourceLocation slotId, ItemStack extractedStack, RiderConfig config) {
            super(player, slotId, extractedStack, config);
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class Post extends SlotExtractionEvent {
        public Post(Player player, ResourceLocation slotId, ItemStack extractedStack, RiderConfig config) {
            super(player, slotId, extractedStack, config);
        }
    }
}
