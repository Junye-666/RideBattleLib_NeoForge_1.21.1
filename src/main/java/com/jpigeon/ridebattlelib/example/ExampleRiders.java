package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.henshin.TriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExampleRiders {
    // 定义测试骑士的ID
    private static final ResourceLocation TEST_RIDER_ALPHA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_alpha");
    private static final ResourceLocation TEST_RIDER_BETA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_beta");

    private static void alphaRider() {
        // 创建骑士配置
        RiderConfig riderAlpha = new RiderConfig(TEST_RIDER_ALPHA)
                .setDriverItem(Items.IRON_LEGGINGS, EquipmentSlot.LEGS) // 驱动器: 铁护腿(穿戴在腿部)
                .setTriggerType(TriggerType.KEY) // 按键触发变身
                .addSlot(
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot"),
                        List.of(Items.IRON_INGOT, Items.GOLD_INGOT),
                        true,
                        true
                ) // 核心槽位: 接受铁锭或金锭(必要槽位)
                .addSlot(
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "energy_slot"),
                        List.of(Items.REDSTONE, Items.GLOWSTONE_DUST),
                        false,
                        false
                ); // 能量槽位: 接受红石或荧石粉(非必要)

        // 创建基础形态配置
        FormConfig alphaBaseForm = new FormConfig(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_base_form"))
                .setArmor(// 设置盔甲
                        Items.IRON_HELMET,
                        Items.IRON_CHESTPLATE,
                        null,
                        Items.IRON_BOOTS
                )
                .addAttribute(// 增加生命值
                        ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"),
                        8.0,
                        AttributeModifier.Operation.ADD_VALUE
                )
                .addAttribute(// 增加移动速度
                        ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"),
                        0.1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
                .addEffect(// 增加夜视效果
                        MobEffects.NIGHT_VISION,
                        114514,
                        0,
                        true
                )
                .addRequiredItem(// 要求核心槽位有铁锭
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot"),
                        Items.IRON_INGOT
                );

// 创建强化形态配置
        FormConfig alphaPoweredForm = new FormConfig(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_powered_form"))
                .setArmor(// 金色盔甲
                        Items.GOLDEN_HELMET,
                        Items.GOLDEN_CHESTPLATE,
                        null,
                        Items.GOLDEN_BOOTS
                )
                .addAttribute(// 更高生命值
                        ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"),
                        12.0,
                        AttributeModifier.Operation.ADD_VALUE
                )
                .addAttribute(// 更高移动速度
                        ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"),
                        0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
                .addEffect(// 增加力量效果
                        MobEffects.DAMAGE_BOOST,
                        114514,
                        0,
                        true
                )
                .addEffect(
                        MobEffects.NIGHT_VISION,
                        114514,
                        0,
                        true
                )
                .addRequiredItem(// 要求核心槽位有金锭
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot"),
                        Items.GOLD_INGOT
                )
                .addRequiredItem(// 要求能量槽位有物品
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "energy_slot"),
                        Items.REDSTONE
                );

// 将形态添加到骑士配置
        riderAlpha
                .addForm(alphaBaseForm)
                .addForm(alphaPoweredForm)
                .setBaseForm(alphaBaseForm.getFormId());// 设置基础形态

// 注册骑士
        RiderRegistry.registerRider(riderAlpha);
    }

    public static void betaRider() {
        RiderConfig riderBeta = new RiderConfig(TEST_RIDER_BETA)
                .setDriverItem(Items.BRICK, EquipmentSlot.OFFHAND)
                .setTriggerType(TriggerType.ITEM)
                .setTriggerItem(Items.REDSTONE)
                .setUniversalGear(EquipmentSlot.FEET, Items.LEATHER_BOOTS)
                .addSlot(ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_head"),
                        List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND), true, false)
                .addSlot(ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_chest"),
                        List.of(Items.LEATHER, Items.IRON_BLOCK, Items.GOLD_BLOCK, Items.DIAMOND_BLOCK), true, false)
                .addSlot(ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_leggings"),
                        List.of(Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.GLOWSTONE_DUST), true, false)
                ;
        FormConfig betaBaseForm = new FormConfig(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_base_form")
        )
                .setArmor(
                        Items.LEATHER_HELMET,
                        Items.LEATHER_CHESTPLATE,
                        Items.LEATHER_LEGGINGS,
                        Items.LEATHER_BOOTS
                )
                .addEffect(MobEffects.JUMP,
                        42315,
                        1,
                        true)
                .addRequiredItem(
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_chest"),
                        Items.LEATHER);

        FormConfig betaDynamicForm = new FormConfig(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_dynamic_form")
        )
                .setArmor(
                        Items.LEATHER_HELMET,
                        Items.LEATHER_CHESTPLATE,
                        Items.LEATHER_LEGGINGS,
                        Items.LEATHER_BOOTS
                );

        Map<Item, Item> headMapping = new HashMap<>();
        headMapping.put(Items.IRON_INGOT, Items.IRON_HELMET);
        headMapping.put(Items.GOLD_INGOT, Items.GOLDEN_HELMET);
        headMapping.put(Items.DIAMOND, Items.DIAMOND_HELMET);

        Map<Item, MobEffectInstance> headEffects = new HashMap<>();
        headEffects.put(Items.IRON_INGOT, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 42315));
        headEffects.put(Items.GOLD_INGOT, new MobEffectInstance(MobEffects.DIG_SPEED, 42315));
        headEffects.put(Items.DIAMOND, new MobEffectInstance(MobEffects.HEALTH_BOOST, 42315));

        betaDynamicForm.addDynamicPart(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_head"), // 修改为beta_slot_head
                EquipmentSlot.HEAD,
                headMapping,
                headEffects
        );


        Map<Item, Item> chestMapping = new HashMap<>();
        chestMapping.put(Items.IRON_BLOCK, Items.IRON_CHESTPLATE);
        chestMapping.put(Items.GOLD_BLOCK, Items.GOLDEN_CHESTPLATE);
        chestMapping.put(Items.DIAMOND_BLOCK, Items.DIAMOND_CHESTPLATE);

        Map<Item, MobEffectInstance> chestEffects = new HashMap<>();
        chestEffects.put(Items.IRON_BLOCK, new MobEffectInstance(MobEffects.ABSORPTION, 42315));
        chestEffects.put(Items.GOLD_BLOCK, new MobEffectInstance(MobEffects.SATURATION, 42315));
        chestEffects.put(Items.DIAMOND_BLOCK, new MobEffectInstance(MobEffects.HEALTH_BOOST, 42315));

        betaDynamicForm.addDynamicPart(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_chest"), // 修改为beta_slot_chest
                EquipmentSlot.CHEST,
                chestMapping,
                chestEffects
        );


        Map<Item, Item> legMapping = new HashMap<>();
        legMapping.put(Items.IRON_NUGGET, Items.IRON_LEGGINGS);
        legMapping.put(Items.GOLD_NUGGET, Items.GOLDEN_LEGGINGS);
        legMapping.put(Items.GLOWSTONE_DUST, Items.DIAMOND_LEGGINGS);

        Map<Item, MobEffectInstance> legEffects = new HashMap<>();
        legEffects.put(Items.IRON_NUGGET, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 42315));
        legEffects.put(Items.GOLD_NUGGET, new MobEffectInstance(MobEffects.SLOW_FALLING, 42315));
        legEffects.put(Items.GLOWSTONE_DUST, new MobEffectInstance(MobEffects.GLOWING, 42315));

        betaDynamicForm.addDynamicPart(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "beta_slot_leggings"), // 修改为beta_slot_leggings
                EquipmentSlot.LEGS, // 保持为FEET，这是装备位置
                legMapping,
                legEffects
        );
        riderBeta
                .addForm(betaBaseForm)
                .addForm(betaDynamicForm)
                .setBaseForm(betaBaseForm.getFormId());
        RiderRegistry.registerRider(riderBeta);
    }

    public static void init() {
        alphaRider();
        betaRider();
    }
}