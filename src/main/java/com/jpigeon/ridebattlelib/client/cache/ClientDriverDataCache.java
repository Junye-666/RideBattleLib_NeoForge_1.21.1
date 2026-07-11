package com.jpigeon.ridebattlelib.client.cache;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientDriverDataCache {
    private static final Map<UUID, Map<ResourceLocation, ItemStack>> MAIN_ITEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<ResourceLocation, ItemStack>> AUX_ITEMS = new ConcurrentHashMap<>();

    public static void setMainItems(UUID playerId, Map<ResourceLocation, ItemStack> items) {
        MAIN_ITEMS.put(playerId, new HashMap<>(items));
    }

    public static void setAuxItems(UUID playerId, Map<ResourceLocation, ItemStack> items) {
        AUX_ITEMS.put(playerId, new HashMap<>(items));
    }

    public static void applyChanges(UUID playerId, Map<ResourceLocation, ItemStack> changes, boolean fullSync) {
        if (fullSync) {
            // 全量同步，直接替换主驱动器数据（假设全量包仅含主驱动器）
            setMainItems(playerId, changes);
            return;
        }

        // 增量更新（需要区分主辅，此处简化，假设所有槽位属于主驱动器）
        Map<ResourceLocation, ItemStack> main = MAIN_ITEMS.computeIfAbsent(playerId, k -> new HashMap<>());
        Map<ResourceLocation, ItemStack> aux = AUX_ITEMS.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<ResourceLocation, ItemStack> entry : changes.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            ItemStack newStack = entry.getValue();

            // 根据槽位是否存在于主缓存来决定更新哪一边
            // 注意：如果同一个 slotId 在两边都不存在，默认放到主驱动器（或根据你的业务逻辑处理）
            if (main.containsKey(slotId) || !aux.containsKey(slotId)) {
                // 如果主中有，或者辅助中没有（防止主没有但辅助有的覆盖），优先更新主
                if (newStack.isEmpty()) main.remove(slotId);
                else main.put(slotId, newStack);
            } else if (aux.containsKey(slotId)) {
                if (newStack.isEmpty()) aux.remove(slotId);
                else aux.put(slotId, newStack);
            }
        }
    }

    public static Map<ResourceLocation, ItemStack> getDriverItems(UUID playerId) {
        // 主驱动器
        Map<ResourceLocation, ItemStack> main = getMainItems(playerId);
        Map<ResourceLocation, ItemStack> all = new HashMap<>(main);

        // 辅助驱动器（仅当装备时）
        Map<ResourceLocation, ItemStack> aux = getAuxItems(playerId);
        if (!aux.isEmpty()) {
            all.putAll(aux);
        }

        return all;
    }

    public static Map<ResourceLocation, ItemStack> getMainItems(UUID playerId) {
        return MAIN_ITEMS.getOrDefault(playerId, new HashMap<>());
    }

    public static Map<ResourceLocation, ItemStack> getAuxItems(UUID playerId) {
        return AUX_ITEMS.getOrDefault(playerId, new HashMap<>());
    }

    public static ItemStack getItem(UUID playerId, ResourceLocation slotId) {
        Map<ResourceLocation, ItemStack> main = MAIN_ITEMS.get(playerId);
        if (main != null && main.containsKey(slotId)) {
            return main.get(slotId);
        }
        Map<ResourceLocation, ItemStack> aux = AUX_ITEMS.get(playerId);
        if (aux != null && aux.containsKey(slotId)) {
            return aux.get(slotId);
        }
        return ItemStack.EMPTY;
    }

    public static void remove(UUID playerId) {
        MAIN_ITEMS.remove(playerId);
        AUX_ITEMS.remove(playerId);
    }

    public static void clear() {
        MAIN_ITEMS.clear();
        AUX_ITEMS.clear();
    }
}