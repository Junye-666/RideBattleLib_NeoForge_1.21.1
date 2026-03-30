package com.jpigeon.ridebattlelib.common.registry;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RiderArmorRegistry {
    private static final Set<Item> RIDER_ARMORS = new HashSet<>();
    private static final Set<Item> RIDER_DRIVERS = new HashSet<>();

    public static void registerRiderArmor(RiderConfig config) {
        RIDER_DRIVERS.add(config.getDriverItem());
        if (config.getAuxDriverItem() != null) RIDER_DRIVERS.add(config.getAuxDriverItem());

        for (FormConfig formConfig : config.getForms().values()){
            if (isValidArmor(config, formConfig.getHelmet())) RIDER_ARMORS.add(formConfig.getHelmet());
            if (isValidArmor(config, formConfig.getChestplate())) RIDER_ARMORS.add(formConfig.getChestplate());
            if (isValidArmor(config, formConfig.getLeggings())) RIDER_ARMORS.add(formConfig.getLeggings());
            if (isValidArmor(config, formConfig.getBoots())) RIDER_ARMORS.add(formConfig.getBoots());
        }
    }

    public static boolean isValidArmor(RiderConfig config, Item item) {
        return item != null && item != Items.AIR && item != config.getDriverItem();
    }

    public static Set<Item> getAllArmor() {
        return Collections.unmodifiableSet(RIDER_ARMORS);
    }
    public static Set<Item> getAllDriver() {
        return Collections.unmodifiableSet(RIDER_DRIVERS);
    }
}
