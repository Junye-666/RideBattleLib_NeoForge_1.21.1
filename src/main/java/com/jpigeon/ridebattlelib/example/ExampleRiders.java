package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.henshin.TriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.List;

public class ExampleRiders {

    public static final RiderConfig TEST_RIDER_ALPHA = new RiderConfig(
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_alpha")
    )
            .setDriverItem(Items.BRICK, EquipmentSlot.OFFHAND)
            .setTriggerType(TriggerType.KEY) // 明确指定按键触发
            .setArmor(
                    Items.GOLDEN_HELMET,
                    Items.GOLDEN_CHESTPLATE,
                    Items.GOLDEN_LEGGINGS,
                    Items.GOLDEN_BOOTS
            )
            .addSlot(
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_slot1"),
                    List.of(Items.GOLD_INGOT),
                    true
            );

    public static final RiderConfig TEST_RIDER_BETA = new RiderConfig(
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_beta")
    )
            .setDriverItem(Items.DIAMOND_LEGGINGS, EquipmentSlot.LEGS)
            .setTriggerType(TriggerType.ITEM) // 指定物品触发
            .setTriggerItem(Items.DIAMOND) // 设置触发物品为钻石
            .setArmor(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    null,
                    Items.DIAMOND_BOOTS
            )
            .addSlot(
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot1"),
                    List.of(Items.STONE, Items.IRON_INGOT),
                    true
            )
            .addSlot(
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot2"),
                    List.of(Items.REDSTONE),
                    false
            );

    public static final RiderConfig TEST_RIDER_GAMMA = new RiderConfig(
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_gamma")
    )
            .setDriverItem(Items.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST)
            .setTriggerType(TriggerType.AUTO) // 自动触发
            .setArmor(
                    Items.NETHERITE_HELMET,
                    Items.NETHERITE_CHESTPLATE,
                    Items.NETHERITE_LEGGINGS,
                    Items.NETHERITE_BOOTS
            )
            .addSlot(
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "gamma_slot1"),
                    List.of(Items.NETHER_STAR),
                    true
            )
            .addSlot(
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "gamma_slot2"),
                    List.of(Items.DRAGON_BREATH),
                    true
            )
            .addSlot(
                    ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "gamma_slot3"),
                    List.of(Items.CHORUS_FRUIT),
                    false
            );

    public static void init() {
        RiderRegistry.registerRider(TEST_RIDER_ALPHA);
        RiderRegistry.registerRider(TEST_RIDER_BETA);
        RiderRegistry.registerRider(TEST_RIDER_GAMMA);
    }
}
