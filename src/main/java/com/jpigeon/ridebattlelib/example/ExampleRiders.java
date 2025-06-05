package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class ExampleRiders {
    // 定义测试骑士的ID
    private static final ResourceLocation TEST_RIDER_ALPHA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_alpha");

    private static void alphaRider() {
        // 创建骑士配置
        RiderConfig riderAlpha = new RiderConfig(TEST_RIDER_ALPHA)
                .setDriverItem(Items.IRON_LEGGINGS, EquipmentSlot.LEGS) // 驱动器: 铁护腿(穿戴在腿部)
                .setTriggerType(TriggerType.KEY) // 指定按键触发
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
                .addEffect(// 墳加夜视效果
                        MobEffects.NIGHT_VISION,
                        114514,
                        0,
                        true
                )
                .addRequiredItem(// 要求核心槽位有铁锭
                        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot"),
                        Items.IRON_INGOT
                )
                .addGrantedItem(Items.IRON_SWORD.getDefaultInstance());

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
                )
                .addGrantedItem(Items.NETHERITE_SWORD.getDefaultInstance());

        // 将形态添加到骑士配置
        riderAlpha
                .addForm(alphaBaseForm)
                .addForm(alphaPoweredForm)
                .setBaseForm(alphaBaseForm.getFormId());// 设置基础形态
        alphaBaseForm.setAllowsEmptyBelt(false);

        // 注册骑士
        RiderRegistry.registerRider(riderAlpha);
    }

    public static void init() {
        alphaRider();
        registerPauseResumeHandler(); // 添加测试用的暂停/继续处理器
    }

    // 测试用的暂停/继续处理器
    private static void registerPauseResumeHandler() {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
            if (event.getEntity().level().isClientSide()) return;

            Player player = event.getEntity();
            ItemStack heldItem = event.getItemStack();

            // 使用钻石右键空气时恢复变身
            if (heldItem.is(Items.DIAMOND)) {
                if (HenshinHelper.isPaused(player, TEST_RIDER_ALPHA)) {
                    HenshinHelper.resumeTransformation(player, TEST_RIDER_ALPHA);
                    player.displayClientMessage(Component.literal("[测试] 继续变身").withStyle(ChatFormatting.GREEN), true);
                    event.setCanceled(true);
                }
            }
        });

        NeoForge.EVENT_BUS.addListener((AnimationEvent event) -> {
            if (event.getPhase() == AnimationPhase.INIT &&
                    event.getRiderId().equals(TEST_RIDER_ALPHA)) {
                // 在初始化阶段暂停变身
                event.setCanceled(true);
                HenshinHelper.pauseTransformation(event.getPlayer(), TEST_RIDER_ALPHA);
                event.getPlayer().displayClientMessage(Component.literal("[测试] 变身已暂停，请使用钻石右键空气继续").withStyle(ChatFormatting.YELLOW), true);
            }
        });
    }
}