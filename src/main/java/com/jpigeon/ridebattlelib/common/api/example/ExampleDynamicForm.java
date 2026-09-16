package com.jpigeon.ridebattlelib.common.api.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.builder.DynamicMappingBuilder;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.config.TriggerType;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Items;

import java.util.List;

public class ExampleDynamicForm {
    public static final ResourceLocation TEST_RIDER_BETA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_beta");

    public static final ResourceLocation BETA_BASE_FORM =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_base_form");

    public static final ResourceLocation BETA_SLOT_1 =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_1");

    public static final ResourceLocation BETA_SLOT_2 =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_2");

    public static final RiderConfig riderBeta = new RiderConfig(TEST_RIDER_BETA)
            .setMainDriverItem(Items.NETHERITE_LEGGINGS, EquipmentSlot.LEGS)
            .addMainDriverSlot(
                    BETA_SLOT_1,
                    List.of(Items.EMERALD, Items.DIAMOND),
                    true,
                    true)
            .addMainDriverSlot(
                    BETA_SLOT_2,
                    List.of(Items.REDSTONE, Items.GLOWSTONE_DUST),
                    true,
                    true)
            .setAllowDynamicForms(true);

    public static final FormConfig baseForm = new FormConfig(BETA_BASE_FORM)
            .setTriggerType(TriggerType.KEY)
            .setArmor(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    null,
                    Items.LEATHER_BOOTS)
            .addRequiredItem(BETA_SLOT_1, Items.AIR)
            .addRequiredItem(BETA_SLOT_2, Items.AIR)
            .addAttribute(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"),
                    8.0,
                    AttributeModifier.Operation.ADD_VALUE);


    public static void betaRider() {
        // 注册RiderConfig
        riderBeta
                .addForm(baseForm)
                .setBaseForm(BETA_BASE_FORM);

        // 允许基础形态以空驱动器变身
        baseForm.setAllowsEmptyDriver(true);

        RiderRegistry.registerRider(riderBeta);
        // registerDynamicMappings();
        buildDynamicMappings();
    }

    private static void buildDynamicMappings() {
        DynamicMappingBuilder.forRider(TEST_RIDER_BETA)
                .armor(Items.DIAMOND, EquipmentSlot.HEAD, Items.DIAMOND_HELMET)
                .effects(Items.DIAMOND, MobEffects.JUMP, MobEffects.ABSORPTION)
                .grantedItem(Items.DIAMOND, Items.DIAMOND_AXE)

                .armor(Items.EMERALD, Items.TURTLE_HELMET)
                .effect(Items.EMERALD, MobEffects.DAMAGE_RESISTANCE)
                .grantedItem(Items.EMERALD, Items.GOLDEN_CARROT)

                .armor(Items.REDSTONE, Items.IRON_CHESTPLATE)
                .effect(Items.REDSTONE, MobEffects.DAMAGE_BOOST)

                .armor(Items.GLOWSTONE_DUST, Items.GOLDEN_CHESTPLATE)
                .effect(Items.GLOWSTONE_DUST, MobEffects.MOVEMENT_SPEED)

                .undersuit(
                        Items.SKELETON_SKULL,
                        Items.CHAINMAIL_CHESTPLATE,
                        null,
                        Items.CHAINMAIL_BOOTS
                )
                .register();
    }

    public static void init() {
        betaRider();
    }
}
