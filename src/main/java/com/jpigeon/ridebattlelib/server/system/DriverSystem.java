package com.jpigeon.ridebattlelib.server.system;

import com.jpigeon.ridebattlelib.common.config.DriverSlotDefinition;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.event.ItemInsertionEvent;
import com.jpigeon.ridebattlelib.common.event.ReturnItemsEvent;
import com.jpigeon.ridebattlelib.common.event.SlotExtractionEvent;
import com.jpigeon.ridebattlelib.server.system.helper.SyncManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DriverSystem {
    private static final DriverSystem INSTANCE = new DriverSystem();

    public static DriverSystem getInstance() {
        return INSTANCE;
    }

    private DriverSystem() {
    }
    //====================核心方法====================

    public boolean insertItem(Player player, ResourceLocation slotId, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0) return false;

        if (player.getCooldowns().isOnCooldown(stack.getItem())) return false;

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return false;

        // 阻止驱动器物品/触发物品
        if (stack.is(config.getDriverItem()) || stack.is(config.getAuxDriverItem()) || (config.getTriggerItem() != null && stack.is(config.getTriggerItem()))) {
            return false;
        }

        boolean isAux = config.getAuxSlotDefinitions().containsKey(slotId);
        DriverSlotDefinition slot = isAux ?
                config.getAuxSlotDefinition(slotId) :
                config.getSlotDefinition(slotId);

        if (slot == null || !slot.allowedItems().contains(stack.getItem())) {
            return false;
        }

        // 触发预插入事件
        ItemInsertionEvent.Pre preEvent = new ItemInsertionEvent.Pre(player, slotId, stack, config);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) return false;

        ItemStack finalStack = preEvent.getStack().copyWithCount(1);
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        // 获取当前骑士的驱动器物品映射（深拷贝）
        Map<ResourceLocation, ItemStack> targetMap = isAux ?
                new HashMap<>(data.getAuxDriverItems().getOrDefault(config.getRiderId(), new HashMap<>())) :
                new HashMap<>(data.getMainDriverItems().getOrDefault(config.getRiderId(), new HashMap<>()));

        // 检查槽位是否被占用
        if (targetMap.containsKey(slotId) && !targetMap.get(slotId).isEmpty()) {
            if (slot.allowReplace()) {
                // 允许替换：先取出旧物品，再插入新物品
                extractItem(player, slotId); // 这会归还旧物品并更新数据
                // 重新获取更新后的目标Map
                targetMap = isAux ?
                        new HashMap<>(data.getAuxDriverItems().getOrDefault(config.getRiderId(), new HashMap<>())) :
                        new HashMap<>(data.getMainDriverItems().getOrDefault(config.getRiderId(), new HashMap<>()));
                targetMap.put(slotId, finalStack);
            } else {
                return false;
            }
        } else {
            targetMap.put(slotId, finalStack);
        }

        // 保存更新后的数据
        if (isAux) {
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> aux = new HashMap<>(data.getAuxDriverItems());
            aux.put(config.getRiderId(), targetMap);
            data.setAuxDriverItems(aux);
        } else {
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> main = new HashMap<>(data.getMainDriverItems());
            main.put(config.getRiderId(), targetMap);
            data.setMainDriverItems(main);
        }

        // 同步
        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncDriverData(serverPlayer);
        }

        NeoForge.EVENT_BUS.post(new ItemInsertionEvent.Post(player, slotId, finalStack, config));
        return true;
    }

    public ItemStack extractItem(Player player, ResourceLocation slotId) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return ItemStack.EMPTY;

        boolean isAux = config.getAuxSlotDefinitions().containsKey(slotId);
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        Map<ResourceLocation, ItemStack> targetMap = isAux ?
                new HashMap<>(data.getAuxDriverItems().getOrDefault(config.getRiderId(), new HashMap<>())) :
                new HashMap<>(data.getMainDriverItems().getOrDefault(config.getRiderId(), new HashMap<>()));

        if (!targetMap.containsKey(slotId) || targetMap.get(slotId).isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = targetMap.get(slotId).copy();

        // 触发预提取事件
        SlotExtractionEvent.Pre preEvent = new SlotExtractionEvent.Pre(player, slotId, extracted, config);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) return ItemStack.EMPTY;

        extracted = preEvent.getExtractedStack();
        targetMap.remove(slotId);

        // 保存更新后的数据
        if (isAux) {
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> aux = new HashMap<>(data.getAuxDriverItems());
            aux.put(config.getRiderId(), targetMap);
            data.setAuxDriverItems(aux);
        } else {
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> main = new HashMap<>(data.getMainDriverItems());
            main.put(config.getRiderId(), targetMap);
            data.setMainDriverItems(main);
        }

        // 同步
        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncDriverData(serverPlayer);
        }

        // 归还物品
        if (!extracted.isEmpty()) {
            if (!player.addItem(extracted.copy())) {
                player.drop(extracted.copy(), false);
            }
        }

        NeoForge.EVENT_BUS.post(new SlotExtractionEvent.Post(player, slotId, extracted, config));
        return extracted;
    }

    /**
     * 退还所有驱动器物品给玩家
     */
    public void returnItems(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        ReturnItemsEvent.Pre preEvent = new ReturnItemsEvent.Pre(player, config);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) return;

        // 获取所有物品并逐个提取
        Map<ResourceLocation, ItemStack> allItems = getDriverItems(player);
        for (ResourceLocation slotId : new ArrayList<>(allItems.keySet())) {
            extractItem(player, slotId);
        }

        NeoForge.EVENT_BUS.post(new ReturnItemsEvent.Post(player, config));
    }

    private void returnItemToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }
//====================Getters====================
    /**
     * 获取玩家当前驱动器所有物品（主+辅）
     */
    public Map<ResourceLocation, ItemStack> getDriverItems(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return new HashMap<>();

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        // 主驱动器
        Map<ResourceLocation, ItemStack> main = data.getMainDriverItems().getOrDefault(config.getRiderId(), new HashMap<>());
        Map<ResourceLocation, ItemStack> all = new HashMap<>(main);

        // 辅助驱动器（仅当装备时）
        if (config.hasAuxDriverEquipped(player)) {
            Map<ResourceLocation, ItemStack> aux = data.getAuxDriverItems().getOrDefault(config.getRiderId(), new HashMap<>());
            all.putAll(aux);
        }

        return all;
    }
}
