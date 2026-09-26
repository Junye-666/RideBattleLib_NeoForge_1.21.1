package com.jpigeon.ridebattlelib.common.registry;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RiderArmorRegistry {
    private static final Set<Item> RIDER_ARMORS = ConcurrentHashMap.newKeySet();
    private static final Set<Item> RIDER_DRIVERS = ConcurrentHashMap.newKeySet();

    public static void registerRiderArmor(RiderConfig config) {
        Item main = config.getDriverItem();
        if (main != null && main != Items.AIR) RIDER_DRIVERS.add(main);

        Item aux = config.getAuxDriverItem();
        if (aux != null && aux != Items.AIR) RIDER_DRIVERS.add(aux);

        for (FormConfig formConfig : config.getForms().values()) {
            addIfValid(RIDER_ARMORS, config, formConfig.getHelmet());
            addIfValid(RIDER_ARMORS, config, formConfig.getChestplate());
            addIfValid(RIDER_ARMORS, config, formConfig.getLeggings());
            addIfValid(RIDER_ARMORS, config, formConfig.getBoots());
        }
    }

    private static void addIfValid(Set<Item> target, RiderConfig config, @Nullable Item item) {
        if (item == null || item == Items.AIR) return;
        if (item == config.getDriverItem() || item == config.getAuxDriverItem()) return;
        target.add(item);
    }

    public static boolean isRiderArmor(Item item) {
        return RIDER_ARMORS.contains(item);
    }

    public static boolean isRiderDriver(Item item) {
        return RIDER_DRIVERS.contains(item);
    }

    public static boolean isValidArmor(RiderConfig config, Item item) {
        return item != null && item != Items.AIR
                && item != config.getDriverItem()
                && item != config.getAuxDriverItem();
    }

    public static Set<Item> getAllArmor() {
        return Collections.unmodifiableSet(RIDER_ARMORS);
    }

    public static Set<Item> getAllDriver() {
        return Collections.unmodifiableSet(RIDER_DRIVERS);
    }
}