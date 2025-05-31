package com.jpigeon.ridebattlelib.core.system.attribute;

import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeCache {
    private static final Map<UUID, AttributeCache> CACHE = new ConcurrentHashMap<>();

    private final Player player;
    private ResourceLocation lastFormId;
    private Map<ResourceLocation, ItemStack> lastBeltItems;

    private AttributeCache(Player player) {
        this.player = player;
    }

    public static AttributeCache get(Player player) {
        return CACHE.computeIfAbsent(player.getUUID(), k -> new AttributeCache(player));
    }

    public boolean requiresUpdate(FormConfig form) {
        if (!form.getFormId().equals(lastFormId)) return true;

        Map<ResourceLocation, ItemStack> current = BeltSystem.INSTANCE.getBeltItems(player);
        if (!current.keySet().equals(lastBeltItems.keySet())) return true;

        for (Map.Entry<ResourceLocation, ItemStack> entry : current.entrySet()) {
            ItemStack last = lastBeltItems.get(entry.getKey());
            if (!ItemStack.matches(last, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public void update(FormConfig form) {
        this.lastFormId = form.getFormId();
        this.lastBeltItems = new HashMap<>(BeltSystem.INSTANCE.getBeltItems(player));
    }
}
