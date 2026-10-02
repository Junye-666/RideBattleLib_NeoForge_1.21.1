package com.jpigeon.ridebattlelib.common.api.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.RideBattleAPI;
import com.jpigeon.ridebattlelib.common.api.builder.FormBuilder;
import com.jpigeon.ridebattlelib.common.api.builder.RiderBuilder;
import com.jpigeon.ridebattlelib.common.api.registry.IRiderPack;
import com.jpigeon.ridebattlelib.common.api.server.IRiderServerHandler;
import com.jpigeon.ridebattlelib.common.api.server.ServerRiderDispatcher;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.TriggerType;
import com.jpigeon.ridebattlelib.server.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.server.event.HenshinEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 示例 1：基础骑士 —— 用 RiderBuilder 一站式构建。
 * <p>
 * 通过 {@link IRiderPack#registerCommon()} 触发注册，主模组构造器里调用
 * {@code RiderPackRegistry.register(new ExampleBasicPack())} 即可。
 */
public class ExampleBasicPack implements IRiderPack {

    public static final ResourceLocation RIDER_ID = rl("test_alpha");
    public static final ResourceLocation FORM_BASE = rl("alpha_base_form");
    public static final ResourceLocation FORM_POWER = rl("alpha_powered_form");
    public static final ResourceLocation CORE_SLOT = rl("core_slot");
    public static final ResourceLocation ENERGY_SLOT = rl("energy_slot");
    public static final ResourceLocation SKILL_ID = rl("test_skill");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, path);
    }

    @Override
    public ResourceLocation riderId() {
        return RIDER_ID;
    }


    public static final FormConfig BASE_FORM = FormBuilder.create(FORM_BASE)
            .armor(Items.IRON_HELMET, Items.IRON_CHESTPLATE, null, Items.IRON_BOOTS)
            .requiredItem(CORE_SLOT, Items.IRON_INGOT)
            .triggerType(TriggerType.KEY)
            .attribute(Attributes.MAX_HEALTH, 8.0, AttributeModifier.Operation.ADD_VALUE)
            .attribute(Attributes.MOVEMENT_SPEED, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .effect(MobEffects.NIGHT_VISION, 0)
            .effect(MobEffects.DIG_SPEED, 1)
            .shouldPause(true)
            .grantedItem(Items.IRON_SWORD)
            .grantedItem(Items.SHIELD)
            .allowsEmptyDriver(false)
            .build();

    public static final FormConfig POWER_FORM = FormBuilder.create(FORM_POWER)
            .armor(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, null, Items.GOLDEN_BOOTS)
            .triggerType(TriggerType.AUTO)
            .attribute(Attributes.MAX_HEALTH, 12.0, AttributeModifier.Operation.ADD_VALUE)
            .attribute(Attributes.MOVEMENT_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .effect(MobEffects.DAMAGE_BOOST, 0)
            .effect(MobEffects.NIGHT_VISION, 0)
            .requiredItem(CORE_SLOT, Items.GOLD_INGOT)
            .auxRequiredItem(ENERGY_SLOT, Items.REDSTONE)
            .grantedItem(Items.NETHERITE_SWORD)
            .skill(SKILL_ID, 20)
            .build();

    @Override
    public void registerCommon() {
        RiderBuilder.create(RIDER_ID)
                .driver(Items.IRON_LEGGINGS)
                .auxDriver(Items.BRICK)
                .slot(CORE_SLOT, List.of(Items.IRON_INGOT, Items.GOLD_INGOT), true, true)
                .auxSlot(ENERGY_SLOT, List.of(Items.REDSTONE, Items.GLOWSTONE_DUST, Items.APPLE), true, false)

                .form(BASE_FORM)

                .form(POWER_FORM)

                .baseForm(FORM_BASE)
                .allowDynamicForms(false)
                .buildAndRegister();

        // 演示用的暂停/继续处理器
        ServerRiderDispatcher.register(new PauseResumeHandler());
    }

    /**
     * 演示：通过服务器监听 HenshinEvent.Pre / FormSwitchEvent.Pre，在暂停阶段手动完成变身。
     */
    private static final class PauseResumeHandler implements IRiderServerHandler {

        @Override
        public ResourceLocation riderId() {
            return RIDER_ID;
        }

        @Override
        public void onHenshinPre(HenshinEvent.Pre event) {
            if (FORM_BASE.equals(event.getFormId())) {
                RideBattleAPI.completeIn(44, event.getPlayer());
            }
        }

        @Override
        public void onSwitchPre(FormSwitchEvent.Pre event) {
            if (FORM_POWER.equals(event.getOldFormId())) {
                event.getPlayer().displayClientMessage(
                        Component.literal("从金形态切换时会出现在物品栏上方的字"), true);
            }
            if (FORM_BASE.equals(event.getNewFormId())) {
                RideBattleAPI.completeIn(44, event.getPlayer());
            }
        }
    }
}
