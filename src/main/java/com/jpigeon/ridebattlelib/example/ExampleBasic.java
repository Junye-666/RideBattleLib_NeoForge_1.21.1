package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.RiderManager;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class ExampleBasic {
    // 定义测试骑士的ID
    private static final ResourceLocation TEST_RIDER_ALPHA =
            ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "test_alpha");

    // 定义测试形态的ID
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
                    List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.ENDER_PEARL),
                    true,
                    true
            ) // 驱动器中的核心槽位: 接受铁锭或金锭(必要槽位)
            .addAuxDriverSlot(
                    TEST_ENERGY_SLOT,
                    List.of(Items.REDSTONE, Items.GLOWSTONE_DUST, Items.APPLE),
                    true,
                    false
            ); // 辅助驱动器中的能量槽位: 接受红石或荧石粉(必要槽位)

    // 创建对应基础形态的FormConfig
    public static final FormConfig alphaBaseForm = new FormConfig(TEST_FORM_BASE)
            .setTriggerType(TriggerType.KEY) // 指定按键触发（默认为按键触发）
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
            .addEffect(// 添加夜视效果
                    MobEffects.NIGHT_VISION,
                    114514,
                    0,
                    true
            )
            .addEffect(// 快捷方式，添加急迫效果
                    MobEffects.DIG_SPEED, 1
            )
            .addRequiredItem(// 要求核心槽位有铁锭
                    TEST_CORE_SLOT,
                    Items.IRON_INGOT
            )
            .setShouldPause(true)
            .addGrantedItem(Items.IRON_SWORD.getDefaultInstance()) // 变身时给予物品（传入ItemStack）
            .addGrantedItem(Items.SHIELD); // 变身时给予物品（传入Item）

    // 创建对应强化形态的FormConfig
    public static final FormConfig alphaPoweredForm = new FormConfig(TEST_FORM_POWERED)
            .setTriggerType(TriggerType.AUTO) // 指定物品存入后自动触发
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
                    MobEffects.DAMAGE_BOOST, 0
            )
            .addEffect(
                    MobEffects.NIGHT_VISION, 0
            )
            .addRequiredItem(// 要求核心槽位有金锭
                    TEST_CORE_SLOT,
                    Items.GOLD_INGOT
            )
            .addAuxRequiredItem(// 要求辅助驱动器内能量槽位有物品
                    TEST_ENERGY_SLOT,
                    Items.REDSTONE
            )
            .addGrantedItem(Items.NETHERITE_SWORD);


    private static void registerAlphaRider() {
        // 将形态添加到骑士配置
        riderAlpha
                .addForm(alphaBaseForm) //添加形态
                .addForm(alphaPoweredForm)
                .setBaseForm(alphaBaseForm.getFormId());// 设置基础形态

        alphaBaseForm.setAllowsEmptyDriver(false); // 指定驱动器物品的必要性

        // 注册骑士（核心步骤！）
        RiderRegistry.registerRider(riderAlpha);
    }

    public static void init() {
        registerAlphaRider();
        registerPauseResumeHandler(); // 添加演示用的暂停/继续处理器
    }

    // 演示用的暂停/继续处理器
    private static void registerPauseResumeHandler() {
        NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onHenshinPre(HenshinEvent.Pre event) { // 监听变身事件
                Player player = event.getPlayer();

                if (event.getFormId().equals(TEST_FORM_BASE)) { // 仅为基础形态触发
                    RiderManager.scheduleSeconds( // 等待2.21秒后触发completeHenshin
                            2.21F,
                            () -> RiderManager.completeHenshin(player)
                    );
                }
            }

            @SubscribeEvent
            public void onSwitchForm(FormSwitchEvent.Pre event) { // 监听形态切换事件
                if (event.getOldFormId().equals(TEST_FORM_POWERED)) { // 仅当切换形态前为金色形态时
                    event.getPlayer().displayClientMessage(Component.literal("从金形态切换时会出现在物品栏上方的字"), true);
                }
            }
        });
    }
}
