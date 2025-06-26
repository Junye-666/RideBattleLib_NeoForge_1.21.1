package com.jpigeon.ridebattlelib.core.system.belt;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IBeltSystem;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.network.packet.BeltDataDiffPacket;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.network.packet.BeltDataSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
  * 腰带系统
  * 管理腰带内部物品的存储和操作
 */
public class BeltSystem implements IBeltSystem {
    public static final BeltSystem INSTANCE = new BeltSystem();

    //====================核心方法====================

    // 存入物品
    @Override
    public boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0) {
            RideBattleLib.LOGGER.error("无法插入无效物品");
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
        if (slot == null || !slot.allowedItems().contains(stack.getItem())) {
            return false;
        }

        Map<ResourceLocation, ItemStack> playerBelt = getBeltItems(player);

        // 检查槽位是否被占用
        if (playerBelt.containsKey(slotId)) {
            ItemStack existing = playerBelt.get(slotId);
            if (!existing.isEmpty()) {
                if (slot.allowReplace()) {
                    // 返还旧物品（只返还一个）
                    returnItemToPlayer(player, existing.copyWithCount(1)); // 关键修改：只返还一个

                    // 插入新物品（只插入一个）
                    ItemStack toInsert = stack.copyWithCount(1); // 关键修改：只插入一个
                    playerBelt.put(slotId, toInsert);
                    setBeltItems(player, playerBelt);
                    syncBeltData(player);

                    // 减少玩家手中物品数量（只减少一个）
                    stack.shrink(1);
                    return true;
                } else {
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

            // 根据触发类型分发事件
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config != null) {
                switch (config.getTriggerType()) {
                    case AUTO:
                        BeltHandler.onAutoItemExtracted(player, slotId);
                        break;
                    case ITEM:
                        // ITEM 类型通常不需要响应提取事件
                        break;
                    case KEY:
                        // KEY 类型通常不需要响应提取事件
                        break;
                }
            }
        }
        return extracted;
    }

    @Override
    public void returnItems(Player player) {
        Map<ResourceLocation, ItemStack> items = getBeltItems(player);

        items.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .forEach(entry -> returnItemToPlayer(player, entry.getValue()));

        setBeltItems(player, new HashMap<>());
        syncBeltData(player);
    }

    private void returnItemToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    @Override
    public boolean isValidForSlot(Player player, ResourceLocation slotId, ItemStack stack) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return false;

        SlotDefinition slot = config.getSlotDefinition(slotId);
        return slot != null && slot.allowedItems().contains(stack.getItem());
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
                    slotId, item.getItem(), slot.allowedItems()
            );

            if (item.isEmpty() || !slot.allowedItems().contains(item.getItem())) {
                return false;
            }
        }
        return true;
    }
    //====================Getters====================

    @Override
    public Map<ResourceLocation, ItemStack> getBeltItems(Player player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        // 根据当前激活的骑士获取腰带数据
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return new HashMap<>();

        return new HashMap<>(data.getBeltItems(config.getRiderId()));
    }

    @Override
    public void setBeltItems(Player player, Map<ResourceLocation, ItemStack> items) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);

        // 根据当前激活的骑士设置腰带数据
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems),
                oldData.transformedData(),
                oldData.getHenshinState(),
                oldData.getPendingFormId(),
                oldData.getPenaltyCooldownEnd()
        );

        newData.setBeltItems(config.getRiderId(), items);

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }

    //====================网络通信方法====================

    public void syncBeltData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Map<ResourceLocation, ItemStack> currentItems = getBeltItems(player);
            PacketHandler.sendToClient(serverPlayer, new BeltDataSyncPacket(
                    player.getUUID(),
                    new HashMap<>(currentItems)
            )
            );
        }
    }

    // 客户端应用差异包
    public void applyDiffPacket(BeltDataDiffPacket packet) {
        Player player = findPlayer(packet.playerId());
        if (player == null) return;

        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        ResourceLocation riderId = config.getRiderId();

        // 创建新数据（深拷贝）
        Map<ResourceLocation, Map<ResourceLocation, ItemStack>> newRiderBeltItems =
                new HashMap<>();
        oldData.riderBeltItems.forEach((id, items) ->
                newRiderBeltItems.put(id, new HashMap<>(items))
        );

        // 获取当前骑士的腰带数据
        Map<ResourceLocation, ItemStack> currentItems =
                new HashMap<>(newRiderBeltItems.getOrDefault(riderId, new HashMap<>()));

        // 应用变更
        if (packet.fullSync()) {
            currentItems = new HashMap<>(packet.changes());
        } else {
            Map<ResourceLocation, ItemStack> finalCurrentItems = currentItems;
            packet.changes().forEach((slotId, stack) -> {
                if (stack.isEmpty()) {
                    finalCurrentItems.remove(slotId);
                } else {
                    finalCurrentItems.put(slotId, stack);
                }
            });
        }

        // 更新数据
        newRiderBeltItems.put(riderId, currentItems);
        player.setData(ModAttachments.PLAYER_DATA,
                new PlayerPersistentData(
                        newRiderBeltItems,
                        oldData.transformedData(),
                        oldData.getHenshinState(),
                        oldData.getPendingFormId(),
                        oldData.getPenaltyCooldownEnd())
        );
    }

    private Player findPlayer(UUID playerId) {
        if (Minecraft.getInstance().level == null) return null;
        return Minecraft.getInstance().level.getPlayerByUUID(playerId);
    }
}
