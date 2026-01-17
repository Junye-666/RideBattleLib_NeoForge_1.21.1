package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 从槽位取出物品事件
 */
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

    /**
     * 强制修改提取出来的物品（比如拔出来后道具能量/本身消失）
     */
    public void setExtractedStack(ItemStack extractedStack) {
        this.extractedStack = extractedStack;
    }

    public void setAir(){
        setExtractedStack(Items.AIR);
    }

    /**
     * 快捷方法
     */
    public void setExtractedStack(Item item){
        setExtractedStack(item.getDefaultInstance());
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

    public RiderConfig getConfig() {
        return config;
    }
    /**
     * 可取消取出（卡里面拔不出来了）
     */
    public static class Pre extends SlotExtractionEvent implements ICancellableEvent {
        public Pre(Player player, ResourceLocation slotId, ItemStack extractedStack, RiderConfig config) {
            super(player, slotId, extractedStack, config);
        }
    }

    public static class Post extends SlotExtractionEvent {
        public Post(Player player, ResourceLocation slotId, ItemStack extractedStack, RiderConfig config) {
            super(player, slotId, extractedStack, config);
        }
    }
}
