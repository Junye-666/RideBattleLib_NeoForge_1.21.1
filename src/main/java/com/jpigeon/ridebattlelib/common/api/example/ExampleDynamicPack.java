package com.jpigeon.ridebattlelib.common.api.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.builder.DynamicMappingBuilder;
import com.jpigeon.ridebattlelib.common.api.builder.RiderBuilder;
import com.jpigeon.ridebattlelib.common.api.registry.IRiderPack;
import com.jpigeon.ridebattlelib.common.config.TriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 示例 2：动态形态骑士 —— 允许物品组合动态生成形态。
 */
public class ExampleDynamicPack implements IRiderPack {

    public static final ResourceLocation RIDER_ID = rl("test_beta");
    public static final ResourceLocation BASE_FORM = rl("beta_base_form");
    public static final ResourceLocation SLOT_1 = rl("beta_slot_1");
    public static final ResourceLocation SLOT_2 = rl("beta_slot_2");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, path);
    }

    @Override
    public ResourceLocation riderId() {
        return RIDER_ID;
    }

    @Override
    public void registerCommon() {
        RiderBuilder.create(RIDER_ID)
                .driver(Items.NETHERITE_LEGGINGS)
                .slot(SLOT_1, List.of(Items.EMERALD, Items.DIAMOND), true, true)
                .slot(SLOT_2, List.of(Items.REDSTONE, Items.GLOWSTONE_DUST), true, true)

                .form(BASE_FORM)
                .armor(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, null, Items.LEATHER_BOOTS)
                .triggerType(TriggerType.KEY)
                .requiredItem(SLOT_1, Items.AIR)
                .requiredItem(SLOT_2, Items.AIR)
                .attribute(Attributes.MAX_HEALTH, 8.0, AttributeModifier.Operation.ADD_VALUE)
                .allowsEmptyDriver(true)
                .end()

                .baseForm(BASE_FORM)
                .allowDynamicForms(true)
                .buildAndRegister();

        // 注册动态映射
        buildDynamicMappings();
    }

    /**
     * 动态形态的“物品 → 盔甲/效果/授予物”映射。
     */
    private static void buildDynamicMappings() {
        DynamicMappingBuilder.forRider(RIDER_ID)
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

                .undersuit(Items.SKELETON_SKULL, Items.CHAINMAIL_CHESTPLATE, null, Items.CHAINMAIL_BOOTS)
                .register();
    }
}