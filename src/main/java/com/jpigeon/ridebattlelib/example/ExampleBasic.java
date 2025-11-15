package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class ExampleBasic {
    // 定义测试骑士的ID
    private static final ResourceLocation TEST_RIDER_ALPHA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_alpha");

    private static final ResourceLocation TEST_FORM_BASE =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_base_form");

    private static final ResourceLocation TEST_FORM_POWERED =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_powered_form");

    // 定义槽位ID
    private static final ResourceLocation TEST_CORE_SLOT =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot");
    private static final ResourceLocation TEST_ENERGY_SLOT =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "energy_slot");

    // 定义RiderConfig
    public static final RiderConfig riderAlpha = new RiderConfig(TEST_RIDER_ALPHA)
            .setMainDriverItem(Items.IRON_LEGGINGS, EquipmentSlot.LEGS) // 驱动器: 铁护腿(穿戴在腿部)
            .setAuxDriverItem(Items.BRICK, EquipmentSlot.OFFHAND) // 辅助驱动器: 砖块(穿戴在副手)
            .addMainDriverSlot(
                    TEST_CORE_SLOT,
                    List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND),
                    true,
                    true
            ) // 驱动器中的核心槽位: 接受铁锭或金锭(必要槽位)
            .addAuxDriverSlot(
                    TEST_ENERGY_SLOT,
                    List.of(Items.REDSTONE, Items.GLOWSTONE_DUST, Items.APPLE),
                    true,
                    false
            ); // 辅助驱动器中的能量槽位: 接受红石或荧石粉(非必要)

    // 创建基础形态配置
    public static final FormConfig alphaBaseForm = new FormConfig(TEST_FORM_BASE)
            .setTriggerType(TriggerType.KEY) // 指定按键触发
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
                    TEST_CORE_SLOT,
                    Items.IRON_INGOT
            )
            .addGrantedItem(Items.IRON_SWORD.getDefaultInstance()); // 变身时给予物品

    // 创建强化形态配置
    public static final FormConfig alphaPoweredForm = new FormConfig(TEST_FORM_POWERED)
            .setTriggerType(TriggerType.AUTO) // 指定自动触发
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
                    TEST_CORE_SLOT,
                    Items.GOLD_INGOT
            )
            .addAuxRequiredItem(// 要求辅助驱动器内能量槽位有物品
                    TEST_ENERGY_SLOT,
                    Items.REDSTONE
            )
            .addGrantedItem(Items.NETHERITE_SWORD.getDefaultInstance());



    private static void registerAlphaRider() {
        // 将形态添加到骑士配置
        riderAlpha
                .addForm(alphaBaseForm) //添加形态
                .addForm(alphaPoweredForm)
                .setBaseForm(alphaBaseForm.getFormId());// 设置基础形态

        alphaBaseForm.setAllowsEmptyDriver(false); // 指定驱动器物品的必要性
        alphaBaseForm.setShouldPause(false); // 指定是否在驱动器键按下时暂停

        alphaPoweredForm.setShouldPause(false);

        // 注册骑士
        RiderRegistry.registerRider(riderAlpha);
    }

    public static void init() {
        registerAlphaRider();
        registerPauseResumeHandler(); // 添加测试用的暂停/继续处理器
    }

    // 测试用的暂停/继续处理器
    private static void registerPauseResumeHandler() {
        NeoForge.EVENT_BUS.register(new Object() {
            // 监听按键事件测试强制完成
            @SubscribeEvent
            public void onHenshinPre(HenshinEvent.Pre event) {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                if (player == null) return;

                if (event.getRiderId().equals(TEST_RIDER_ALPHA)) {
                    RiderData data = player.getData(RiderAttachments.RIDER_DATA);
                    if (data.getHenshinState() == HenshinState.TRANSFORMING) {
                        DriverActionManager.INSTANCE.completeTransformation(player);
                    }
                }
            }

            @SubscribeEvent
            public void onSwitchForm(FormSwitchEvent.Pre event){
                if (event.getOldFormId().equals(TEST_FORM_POWERED)){
                    event.getPlayer().displayClientMessage(Component.literal(event.getOldFormId().toString()), false);
                }
            }
        });
    }
}
