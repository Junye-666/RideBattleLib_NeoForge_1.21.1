package com.jpigeon.ridebattlelib.core.system.belt;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IBeltSystem;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.BeltDataSyncPacket;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeltSystem implements IBeltSystem {
    // 存储玩家的腰带数据
    // public static final Map<UUID, Map<ResourceLocation, ItemStack>> beltData = new HashMap<>();
    public static final BeltSystem INSTANCE = new BeltSystem();

    //====================核心方法====================

    // 存入物品
    @Override
    public boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack) {
        if (stack.isEmpty()) {
            RideBattleLib.LOGGER.error("无法插入空物品");
            return false;
        }

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

        Map<ResourceLocation, ItemStack> playerBelt = getBeltItems(player);

        // 检查槽位是否被占用
        if (playerBelt.containsKey(slotId)) {
            ItemStack existing = playerBelt.get(slotId);
            if (!existing.isEmpty()) {
                if (slot.isAllowReplace()) {
                    // 返还旧物品
                    returnItemToPlayer(player, existing);
                } else {
                    // 禁止替换
                    return false;
                }
            }
        }

        // 插入新物品
        playerBelt.put(slotId, stack.copy());
        setBeltItems(player, playerBelt);
        syncBeltData(player);

        return true;
    }

    // 提取物品
    @Override
    public ItemStack extractItem(Player player, ResourceLocation slotId) {
        Map<ResourceLocation, ItemStack> playerBelt = getBeltItems(player);
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

        // 明确遍历所有槽位（包括必要和非必要）
        items.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .forEach(entry -> {
                    ResourceLocation slotId = entry.getKey();
                    ItemStack stack = entry.getValue();
                    returnItemToPlayer(player, stack);
                    RideBattleLib.LOGGER.debug("返还物品: {} -> {}", slotId, stack.getItem());
                });

        // 清空数据并同步
        setBeltItems(player, new HashMap<>());
        syncBeltData(player);
    }

    private void returnItemToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    //====================检测方法====================

    @Override
    public boolean validateItems(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        for (ResourceLocation slotId : config.getRequiredSlots()) {
            ItemStack item = getBeltItems(player).get(slotId);
            SlotDefinition slot = config.getSlotDefinition(slotId);

            // 详细日志输出
            RideBattleLib.LOGGER.info("验证槽位: {} | 物品: {} | 允许物品: {}",
                    slotId, item.getItem(), slot.getAllowedItems()
            );

            if (item.isEmpty() || !slot.getAllowedItems().contains(item.getItem())) {
                return false;
            }
        }
        return true;
    }
    //====================Getters====================

    @Override
    public Map<ResourceLocation, ItemStack> getBeltItems(Player player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        return new HashMap<>(data.beltItems());
    }

    public void setBeltItems(Player player, Map<ResourceLocation, ItemStack> items) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);
        player.setData(ModAttachments.PLAYER_DATA,
                new PlayerPersistentData(new HashMap<>(items), oldData.transformedData()));
    }

    //====================网络通信方法====================

    public void syncBeltData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 创建数据副本避免并发修改
            Map<ResourceLocation, ItemStack> items = new HashMap<>(getBeltItems(player));
            RideBattleLib.LOGGER.debug("同步腰带数据到客户端: {}", items);
            PacketHandler.sendToClient(serverPlayer, new BeltDataSyncPacket(player.getUUID(), items));
        }
    }
}
