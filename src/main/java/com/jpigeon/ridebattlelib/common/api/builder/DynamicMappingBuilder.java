package com.jpigeon.ridebattlelib.common.api.builder;

import com.jpigeon.ridebattlelib.common.config.DynamicFormConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class DynamicMappingBuilder {
    private final ResourceLocation riderId;
    private final List<Runnable> registrations = new ArrayList<>();

    private DynamicMappingBuilder(ResourceLocation riderId) {
        this.riderId = riderId;
    }

    /**
     * 为指定骑士创建构建器
     */
    public static DynamicMappingBuilder forRider(ResourceLocation riderId) {
        return new DynamicMappingBuilder(riderId);
    }

    // ========== 物品→盔甲映射 ==========

    /**
     * 注册物品到指定槽位的盔甲映射
     */
    public DynamicMappingBuilder armor(Item source, EquipmentSlot slot, Item armor) {
        registrations.add(() -> DynamicFormConfig.registerItemArmor(source, slot, armor));
        return this;
    }

    /**
     * 注册物品到盔甲的映射（自动推断槽位）
     */
    public DynamicMappingBuilder armor(Item source, Item armor) {
        registrations.add(() -> DynamicFormConfig.registerItemArmor(source, armor));
        return this;
    }

    /**
     * 批量注册物品到盔甲的映射（自动推断槽位）
     */
    public DynamicMappingBuilder armors(Item source, Item... armors) {
        for (Item armor : armors) {
            registrations.add(() -> DynamicFormConfig.registerItemArmor(source, armor));
        }
        return this;
    }

    // ========== 物品→效果映射 ==========

    /**
     * 注册物品附带的效果
     */
    public DynamicMappingBuilder effect(Item source, Holder<MobEffect> effect) {
        registrations.add(() -> DynamicFormConfig.registerItemEffect(source, effect));
        return this;
    }

    /**
     * 注册物品附带的效果（可自定义持续时间和等级）
     */
    public DynamicMappingBuilder effect(Item source, Holder<MobEffect> effect, int duration, int amplifier) {
        registrations.add(() -> DynamicFormConfig.registerItemEffect(source, effect, duration, amplifier, false));
        return this;
    }

    /**
     * 批量注册物品附带的效果
     */
    @SafeVarargs
    public final DynamicMappingBuilder effects(Item source, Holder<MobEffect>... effects) {
        for (Holder<MobEffect> effect : effects) {
            registrations.add(() -> DynamicFormConfig.registerItemEffect(source, effect));
        }
        return this;
    }

    // ========== 物品→授予物品映射 ==========

    /**
     * 注册物品被放入驱动器后授予的物品
     */
    public DynamicMappingBuilder grantedItems(Item source, ItemStack... granted) {
        registrations.add(() -> DynamicFormConfig.registerItemGrantedItems(source, granted));
        return this;
    }

    /**
     * 注册物品被放入驱动器后授予的物品（快捷方法）
     */
    public DynamicMappingBuilder grantedItem(Item source, Item granted, int count) {
        return grantedItems(source, new ItemStack(granted, count));
    }

    public DynamicMappingBuilder grantedItem(Item source, Item granted) {
        return grantedItem(source, granted, 1);
    }

    // ========== 底衣配置 ==========

    /**
     * 为当前骑士注册底衣配置
     */
    public DynamicMappingBuilder undersuit(Item helmet, Item chestplate, Item leggings, Item boots) {
        registrations.add(() -> DynamicFormConfig.registerRiderUndersuit(
                riderId,
                helmet != null ? helmet : Items.AIR,
                chestplate != null ? chestplate : Items.AIR,
                leggings != null ? leggings : Items.AIR,
                boots != null ? boots : Items.AIR
        ));
        return this;
    }

    /**
     * 底衣配置（使用盔甲数组）
     */
    public DynamicMappingBuilder undersuit(Item... slots) {
        if (slots.length == 4) {
            return undersuit(slots[0], slots[1], slots[2], slots[3]);
        }
        throw new IllegalArgumentException("Undersuit requires exactly 4 items (helmet, chestplate, leggings, boots)");
    }

    // ========== 执行注册 ==========

    /**
     * 执行所有注册操作
     * <p>
     * 注意：此方法应在 FMLClientSetupEvent 或 CommonSetupEvent 中调用
     */
    public void register() {
        for (Runnable task : registrations) {
            task.run();
        }
        registrations.clear(); // 防止重复注册（但保留清空后无法再次调用）
    }

    /**
     * 获取当前已注册的任务数量（调试用）
     */
    public int getPendingCount() {
        return registrations.size();
    }
}
