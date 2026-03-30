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
        for (Map.Entry<ResourceLocation, ItemStack> entry : changes.entrySet()) {
            if (entry.getValue().isEmpty()) {
                main.remove(entry.getKey());
            } else {
                main.put(entry.getKey(), entry.getValue());
            }
        }
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