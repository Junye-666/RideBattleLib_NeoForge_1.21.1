package com.jpigeon.ridebattlelib.core.system.belt;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IBeltSystem;
import com.jpigeon.ridebattlelib.core.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.network.packet.BeltDataSyncPacket;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeltSystem implements IBeltSystem {
    // 存储玩家的腰带数据
    public static final Map<UUID, Map<ResourceLocation, ItemStack>> beltData = new HashMap<>();
    public static final BeltSystem INSTANCE = new BeltSystem();

    //====================核心方法====================

    // 存入物品
    @Override
    public boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack) {
        if (stack.isEmpty()) {
            RideBattleLib.LOGGER.error("无法插入空物品");
            return false;
        }

        Map<ResourceLocation, ItemStack> playerBelt = beltData.computeIfAbsent(
                player.getUUID(),
                k -> new HashMap<>()
        );

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return false;

        // 阻止驱动器物品
        if (stack.is(config.getDriverItem())) {
            return false;
        }

        // 阻止触发物品
        if (stack.is(config.getTriggerItem())) {
            return false;
        }

        SlotDefinition slot = config.getSlotDefinition(slotId);
        if (slot == null || !slot.getAllowedItems().contains(stack.getItem())) {
            return false;
        }

        // 获取当前槽位物品
        ItemStack existingStack = playerBelt.get(slotId);

        // 创建新物品的独立副本
        ItemStack newStack = stack.copy();
        newStack.setCount(1); // 确保只插入一个物品

        // 处理槽位已有物品的情况
        if (existingStack != null && !existingStack.isEmpty()) {
            // 返还旧物品
            returnItemToPlayer(player, existingStack.copy());
            RideBattleLib.LOGGER.debug("返还旧物品: {} -> {}", slotId, existingStack.getItem());
        }

        // 存入新物品
        playerBelt.put(slotId, newStack);
        syncBeltData(player);

        RideBattleLib.LOGGER.debug("插入新物品到槽位: {} -> {}", slotId, newStack.getItem());
        return true;
    }

    // 提取物品
    @Override
    public ItemStack extractItem(Player player, ResourceLocation slotId) {
        Map<ResourceLocation, ItemStack> playerBelt = beltData.get(player.getUUID());
        if (playerBelt == null) return ItemStack.EMPTY;

        ItemStack extracted = playerBelt.remove(slotId);
        if (!extracted.isEmpty()) {
            returnItemToPlayer(player, extracted);
            syncBeltData(player);
        }
        return extracted;
    }

    public void returnItems(Player player) {
        Map<ResourceLocation, ItemStack> items = getBeltItems(player);
        UUID playerId = player.getUUID();

        RideBattleLib.LOGGER.debug("返还玩家 {} 的腰带物品: {}", player.getName(), items);

        // 返还所有非空气物品
        items.forEach((slotId, stack) -> {
            if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
                returnItemToPlayer(player, stack.copy());
                RideBattleLib.LOGGER.debug("返还物品: {} -> {}", slotId, stack.getItem());
            }
        });

        // 清空腰带数据并同步
        beltData.remove(playerId);
        syncBeltData(player);
        saveBeltData(player); // 保存空状态
    }

    private void returnItemToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    //====================检测方法====================

    @Override
    public boolean validateItems(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        Map<ResourceLocation, ItemStack> beltItems = getBeltItems(player);

        for (ResourceLocation slotId : config.getRequiredSlots()) {
            ItemStack item = beltItems.get(slotId);
            SlotDefinition slot = config.getSlotDefinition(slotId);

            if (item == null || item.isEmpty()) {
                RideBattleLib.LOGGER.debug("验证失败: 槽位 {} 为空", slotId);
                return false;
            }

            if (!slot.getAllowedItems().contains(item.getItem())) {
                RideBattleLib.LOGGER.debug("验证失败: 槽位 {} 物品 {} 不在允许列表中",
                        slotId, item.getItem());
                return false;
            }
        }
        return true;
    }

    // 检查槽位是否已被占用
    @Override
    public boolean isSlotOccupied(Player player, ResourceLocation slotId) {
        Map<ResourceLocation, ItemStack> playerBelt = beltData.computeIfAbsent(
                player.getUUID(),
                k -> new HashMap<>()
        );
        ItemStack existingStack = playerBelt.get(slotId);
        RideBattleLib.LOGGER.debug("槽位 {} 已被占用，自动返还物品", slotId);
        return existingStack != null && !existingStack.isEmpty();
    }
    //====================Getters====================

    @Override
    public Map<ResourceLocation, ItemStack> getBeltItems(Player player) {
        Map<ResourceLocation, ItemStack> items = beltData.getOrDefault(player.getUUID(), new HashMap<>());
        return new HashMap<>(items);
    }

    //====================网络通信方法====================

    public void syncBeltData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Map<ResourceLocation, ItemStack> items = new HashMap<>(getBeltItems(player));

            // 调试日志：记录物品详情
            items.forEach((slotId, stack) -> {
                RideBattleLib.LOGGER.debug("同步槽位 {}: {}x{}",
                        slotId, stack.getCount(), stack.getItem());
            });

            PacketHandler.sendToClient(serverPlayer, new BeltDataSyncPacket(player.getUUID(), items));
        }
    }

    public void saveBeltData(Player player) {
        if (player instanceof ServerPlayer) {
            // 传入玩家对象用于序列化
            player.getPersistentData().put("RideBattleBelt",
                    serializeBeltData(getBeltItems(player), player));
        }
    }

    public void loadBeltData(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound("RideBattleBelt");
        if (!tag.isEmpty()) {
            Map<ResourceLocation, ItemStack> savedItems = deserializeBeltData(tag, player);
            Map<ResourceLocation, ItemStack> currentItems = beltData.computeIfAbsent(
                    player.getUUID(),
                    k -> new HashMap<>()
            );

            // 合并数据：保留当前数据，只覆盖NBT中存在的槽位
            savedItems.forEach((slotId, stack) -> {
                // 只添加有效的物品栈
                if (!stack.isEmpty() || stack.getItem() != Items.AIR) {
                    currentItems.put(slotId, stack);
                }
            });
        }
    }

    private CompoundTag serializeBeltData(Map<ResourceLocation, ItemStack> items, Player player) {
        CompoundTag tag = new CompoundTag();
        items.forEach((slotId, stack) -> {
            CompoundTag itemTag = new CompoundTag();
            if (!stack.isEmpty()) {
                stack.save(player.registryAccess(), itemTag);
            } else {
                // 明确标记为空物品栈
                itemTag.putString("id", "minecraft:air");
                itemTag.putInt("Count", 0);
            }
            tag.put(slotId.toString(), itemTag);
        });
        return tag;
    }

    private Map<ResourceLocation, ItemStack> deserializeBeltData(CompoundTag tag, Player player) {
        Map<ResourceLocation, ItemStack> items = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation slotId = ResourceLocation.tryParse(key);
            if (slotId != null) {
                CompoundTag itemTag = tag.getCompound(key);

                // 处理空物品栈的特殊情况
                if (itemTag.contains("id")) {
                    ItemStack stack = ItemStack.parse(player.registryAccess(), itemTag)
                            .orElse(ItemStack.EMPTY);
                    items.put(slotId, stack);
                } else {
                    // 兼容旧格式的空物品
                    items.put(slotId, ItemStack.EMPTY);
                }
            }
        }
        return items;
    }
}
