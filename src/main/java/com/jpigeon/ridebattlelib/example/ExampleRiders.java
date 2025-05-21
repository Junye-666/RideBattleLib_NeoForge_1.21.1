package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public class ExampleRiders {
    public static final RiderConfig EXAMPLE_RIDER_ICHIGO = new RiderConfig(
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "example1")
    )
            .setDriverItem(Items.LEATHER_LEGGINGS, EquipmentSlot.LEGS)
            .setRequiredItem(Items.IRON_INGOT)
            .setArmor(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    null,
                    Items.IRON_BOOTS
            );

    public static final RiderConfig EXAMPLE_RIDER_NIGO = new RiderConfig(
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "example2")
    )
            .setDriverItem(Items.BRICK, EquipmentSlot.OFFHAND)
            .setRequiredItem(Items.GOLD_INGOT)
            .setArmor(
                    Items.GOLDEN_HELMET,
                    Items.GOLDEN_CHESTPLATE,
                    Items.GOLDEN_LEGGINGS,
                    Items.GOLDEN_BOOTS
            );


    public static void init(){
        RiderRegistry.registerRider(EXAMPLE_RIDER_ICHIGO);
        RiderRegistry.registerRider(EXAMPLE_RIDER_NIGO);
    }
}
