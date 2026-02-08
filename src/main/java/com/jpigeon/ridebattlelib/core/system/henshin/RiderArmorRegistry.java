package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
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

        for (FormConfig formConfig : config.forms.values()){
            if (isValidArmor(config, formConfig.getHelmet())) RIDER_ARMORS.add(formConfig.getHelmet());
            if (isValidArmor(config, formConfig.getChestplate())) RIDER_ARMORS.add(formConfig.getChestplate());
            if (isValidArmor(config, formConfig.getLeggings())) RIDER_ARMORS.add(formConfig.getLeggings());
            if (isValidArmor(config, formConfig.getBoots())) RIDER_ARMORS.add(formConfig.getBoots());
        }
    }

    public static boolean isValidArmor(RiderConfig config, Item item) {
        boolean b = item != Items.AIR && item != null;
        if (item != config.getDriverItem()) b = true;
        return b;
    }

    public static Set<Item> getAllArmor() {
        return Collections.unmodifiableSet(RIDER_ARMORS);
    }
    public static Set<Item> getAllDriver() {
        return Collections.unmodifiableSet(RIDER_DRIVERS);
    }
}
