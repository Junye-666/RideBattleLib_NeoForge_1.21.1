package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;
import com.jpigeon.ridebattlelib.system.rider.basic.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public class ExampleRider {
    public static final RiderConfig EXAMPLE_RIDER = new RiderConfig(
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "example")
    )
            .setArmor(
                    Items.GOLDEN_HELMET,
                    Items.GOLDEN_CHESTPLATE,
                    null,
                    Items.GOLDEN_BOOTS
            )
            .setDriverItem(Items.NETHERITE_LEGGINGS, EquipmentSlot.LEGS)
            .setRequiredItem(Items.GOLD_INGOT);


    public static void init(){
        RiderRegistry.registerRider(EXAMPLE_RIDER);
    }
}
