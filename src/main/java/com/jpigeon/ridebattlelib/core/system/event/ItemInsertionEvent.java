package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 存入物品事件
 */
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

    public static class Pre extends ItemInsertionEvent implements ICancellableEvent {
        public Pre(Player player, ResourceLocation slotId, ItemStack stack, RiderConfig config) {
            super(player, slotId, stack, config);
        }
    }

    public static class Post extends ItemInsertionEvent {
        public Post(Player player, ResourceLocation slotId, ItemStack stack, RiderConfig config) {
            super(player, slotId, stack, config);
        }
    }

    /**
     * 强制修改玩家插入的物品(相当于放进去就不是之前拿手上的那个物品了)
     * @param stack 顶替存入的物品
     */
    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    /**
     * 快捷方法
     */
    public void setStack(Item item){
        setStack(item.getDefaultInstance());
    }

    public ItemStack getStack() {
        return stack;
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getSlotId() {
        return slotId;
    }

    public RiderConfig getConfig() {
        return config;
    }
}
