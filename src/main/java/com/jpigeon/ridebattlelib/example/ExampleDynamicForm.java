package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.DynamicArmorRegistry;
import com.jpigeon.ridebattlelib.core.system.form.DynamicEffectRegistry;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
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

    public static void register() {
        // 注册RiderConfig
        RiderConfig riderBeta = new RiderConfig(TEST_RIDER_BETA)
                .setDriverItem(Items.NETHERITE_LEGGINGS, EquipmentSlot.LEGS)
                .addDriverSlot(
                        BETA_SLOT_1,
                        List.of(Items.EMERALD, Items.DIAMOND),
                        true,
                        false)
                .addDriverSlot(
                        BETA_SLOT_2,
                        List.of(Items.REDSTONE, Items.GLOWSTONE_DUST),
                        true,
                        false);

        FormConfig baseForm = new FormConfig(BETA_BASE_FORM)
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

        riderBeta
                .addForm(baseForm)
                .setBaseForm(BETA_BASE_FORM);

        baseForm.setAllowsEmptyBelt(true);

        RiderRegistry.registerRider(riderBeta);
        registerDynamicMappings();
    }

    private static void registerDynamicMappings() {
        // 钻石 -> 钻石头盔 + 跳跃提升 + 伤害吸收
        DynamicArmorRegistry.registerItemArmor(Items.DIAMOND, Items.DIAMOND_HELMET);
        DynamicEffectRegistry.registerItemEffects(Items.DIAMOND, MobEffects.JUMP);
        DynamicEffectRegistry.registerItemEffects(Items.DIAMOND, MobEffects.ABSORPTION);

        // 绿宝石 -> 龟头 + 抗性效果
        DynamicArmorRegistry.registerItemArmor(Items.EMERALD, Items.TURTLE_HELMET);
        DynamicEffectRegistry.registerItemEffects(Items.EMERALD, MobEffects.DAMAGE_RESISTANCE);

        // 红石 -> 铁胸甲 + 伤害提升
        DynamicArmorRegistry.registerItemArmor(Items.REDSTONE, Items.IRON_CHESTPLATE);
        DynamicEffectRegistry.registerItemEffects(Items.REDSTONE, MobEffects.DAMAGE_BOOST);

        // 萤石粉 -> 金甲 + 速度效果
        DynamicArmorRegistry.registerItemArmor(Items.GLOWSTONE_DUST, Items.GOLDEN_CHESTPLATE);
        DynamicEffectRegistry.registerItemEffects(Items.GLOWSTONE_DUST, MobEffects.MOVEMENT_SPEED);
    }

    public static void init() {
        register();
    }
}
