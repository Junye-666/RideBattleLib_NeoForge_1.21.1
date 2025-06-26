package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.trigger.TriggerType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;


import java.util.List;
import java.util.Objects;

public class ExampleRiders {
    // 定义测试骑士的ID
    private static final ResourceLocation TEST_RIDER_ALPHA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_alpha");

    private static final ResourceLocation TEST_FORM_BASE =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_base_form");

    private static final ResourceLocation TEST_FORM_POWERED =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_powered_form");

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
        FormConfig alphaBaseForm = new FormConfig(TEST_FORM_BASE)
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
        FormConfig alphaPoweredForm = new FormConfig(TEST_FORM_POWERED)
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
        alphaBaseForm.setShouldPause(true);
        alphaPoweredForm.setShouldPause(true);

        // 注册骑士
        RiderRegistry.registerRider(riderAlpha);
    }

    public static void init() {
        alphaRider();
        registerPauseResumeHandler(); // 添加测试用的暂停/继续处理器
    }

    // 测试用的暂停/继续处理器
    private static void registerPauseResumeHandler() {
        NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onAnimationEvent(AnimationEvent event) {
                // 只处理测试骑士的事件
                if (!event.getRiderId().equals(TEST_RIDER_ALPHA)) return;

                Player player = event.getPlayer();

                switch (event.getPhase()) {
                    case INIT:
                        // 基础变身动画
                        handleBaseTransformation(event);
                        break;

                    case CONTINUE:
                        // 形态切换动画
                        handleFormSwitch(event);
                        break;

                    case FINALIZE:
                        // 变身完成
                        RideBattleLib.LOGGER.info("{} 的变身动画完成", player.getName().getString());
                        break;
                }
            }

            private void handleBaseTransformation(AnimationEvent event) {
                Player player = event.getPlayer();
                RideBattleLib.LOGGER.info("{} 开始基础变身动画", player.getName().getString());
                    // 3秒后完成变身（模拟动画播放）
                    Objects.requireNonNull(player.level().getServer()).execute(() -> {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignored) {
                        }

                        if (player.isAlive()) {
                            RideBattleLib.LOGGER.info("基础变身动画完成，执行变身");
                            event.completeTransformation();
                        }
                    });
            }

            private void handleFormSwitch(AnimationEvent event) {
                Player player = event.getPlayer();
                ResourceLocation newFormId = event.getFormId();

                String formName = "基础形态";
                if (TEST_FORM_POWERED.equals(newFormId)) {
                    formName = "强化形态";
                }

                RideBattleLib.LOGGER.info("{} 开始切换至 {} 的动画", player.getName().getString(), formName);

                // 2秒后完成形态切换（模拟动画播放）
                Objects.requireNonNull(player.level().getServer()).execute(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }

                    if (player.isAlive()) {
                        RideBattleLib.LOGGER.info("形态切换动画完成，执行切换");
                        event.completeTransformation();
                    }
                });
            }

            // 监听按键事件测试强制完成
            @SubscribeEvent
            public void onKeyInput(InputEvent.Key event) {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                if (player == null) return;

                // 按R键强制完成变身（测试用）
                if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_R)) {
                    PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
                    if (data.getHenshinState() == HenshinState.TRANSFORMING) {
                        RideBattleLib.LOGGER.info("强制完成变身序列");
                        DriverActionManager.INSTANCE.completeTransformation(player);
                    }
                }
            }
        });
    }
}