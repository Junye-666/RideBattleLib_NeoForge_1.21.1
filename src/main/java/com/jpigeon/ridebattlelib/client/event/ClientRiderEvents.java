package com.jpigeon.ridebattlelib.client.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.Map;

/**
 * 客户端事件类，用于客户端
 */
public class ClientRiderEvents {
    // ========== 变身状态变更事件 ==========
    public static class HenshinStateChanged extends Event {
        private final Player player;
        private final boolean isTransformed;
        private final ResourceLocation riderId;
        private final ResourceLocation currentFormId;
        private final ResourceLocation pendingFormId;
        private final ChangeType changeType;

        public enum ChangeType {
            HENSHIN,   // 未变身 -> 已变身
            SWITCH,    // 已变身 -> 切换形态
            UNHENSHIN, // 已变身 -> 解除
            PENDING    // 进入变身缓冲/等待状态
        }

        public HenshinStateChanged(Player player, boolean isTransformed, ResourceLocation riderId, ResourceLocation currentFormId, ResourceLocation pendingFormId, ChangeType changeType) {
            this.player = player;
            this.isTransformed = isTransformed;
            this.riderId = riderId;
            this.currentFormId = currentFormId;
            this.pendingFormId = pendingFormId;
            this.changeType = changeType;
        }

        public Player getPlayer() {
            return player;
        }

        public boolean isTransformed() {
            return isTransformed;
        }

        public ResourceLocation getRiderId() {
            return riderId;
        }

        public ResourceLocation getCurrentFormId() {
            return currentFormId;
        }

        public ResourceLocation getPendingFormId() {
            return pendingFormId;
        }

        public ChangeType getChangeType() {
            return changeType;
        }
    }

    // ========== 驱动器物品变更事件 ==========
    public static class DriverDataChanged extends Event {
        private final Player player;
        private final Map<ResourceLocation, ItemStack> changes;
        private final boolean fullSync;

        public DriverDataChanged(Player player, Map<ResourceLocation, ItemStack> changes, boolean fullSync) {
            this.player = player;
            this.changes = Map.copyOf(changes); // 不可变，安全
            this.fullSync = fullSync;
        }

        public Player getPlayer() {
            return player;
        }

        public Map<ResourceLocation, ItemStack> getChanges() {
            return changes;
        }

        public boolean isFullSync() {
            return fullSync;
        }

        // 快捷方法：如果只插入了一个物品，直接取第一个（diff包通常只有1个）
        public ItemStack getSingleChangedStack() {
            return changes.isEmpty() ? ItemStack.EMPTY : changes.values().iterator().next();
        }

        public ResourceLocation getSingleChangedSlot() {
            return changes.isEmpty() ? null : changes.keySet().iterator().next();
        }
    }
}
