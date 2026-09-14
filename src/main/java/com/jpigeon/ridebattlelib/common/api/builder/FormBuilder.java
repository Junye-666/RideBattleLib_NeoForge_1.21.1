package com.jpigeon.ridebattlelib.common.api.builder;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.TriggerType;
import com.jpigeon.ridebattlelib.server.system.SkillSystem;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 形态构建器 - 用于 KamenRiderBuilder 内部构建 FormConfig
 */
public class FormBuilder {
    private final RiderBuilder parent;
    private final FormConfig form;
    private final List<ResourceLocation> skillIds = new ArrayList<>();

    FormBuilder(RiderBuilder parent, ResourceLocation formId) {
        this.parent = parent;
        this.form = new FormConfig(formId);
    }

    /**
     * 创建一个新的形态构建器
     */
    public static FormBuilder create(ResourceLocation formId) {
        return new FormBuilder(null, formId);
    }

    // ========== 盔甲 ==========

    public FormBuilder armor(@Nullable Item helmet, @Nullable Item chestplate, @Nullable Item leggings, @Nullable Item boots) {
        form.setArmor(helmet, chestplate, leggings, boots);
        return this;
    }

    // ========== 属性 ==========

    public FormBuilder attribute(ResourceLocation attributeId, double amount) {
        form.addAttribute(attributeId, amount);
        return this;
    }

    public FormBuilder attribute(ResourceLocation attributeId, double amount, AttributeModifier.Operation operation) {
        form.addAttribute(attributeId, amount, operation);
        return this;
    }

    // ========== 效果 ==========

    public FormBuilder effect(Holder<MobEffect> effect, int amplifier) {
        form.addEffect(effect, amplifier);
        return this;
    }

    public FormBuilder effect(Holder<MobEffect> effect, int duration, int amplifier, boolean hideParticles) {
        form.addEffect(effect, duration, amplifier, hideParticles);
        return this;
    }

    // ========== 触发类型 ==========

    public FormBuilder triggerType(TriggerType type) {
        form.setTriggerType(type);
        return this;
    }

    // ========== 必需品 ==========

    public FormBuilder requiredItem(String slotPath, Item item) {
        form.addRequiredItem(ResourceLocation.fromNamespaceAndPath(form.getFormId().getNamespace(), slotPath), item);
        return this;
    }

    public FormBuilder requiredItem(ResourceLocation slotId, Item item) {
        form.addRequiredItem(slotId, item);
        return this;
    }

    public FormBuilder auxRequiredItem(String slotPath, Item item) {
        form.addAuxRequiredItem(ResourceLocation.fromNamespaceAndPath(form.getFormId().getNamespace(), slotPath), item);
        return this;
    }

    public FormBuilder auxRequiredItem(ResourceLocation slotId, Item item) {
        form.addAuxRequiredItem(slotId, item);
        return this;
    }

    // ========== 授予物品 ==========

    public FormBuilder grantedItem(Item item) {
        form.addGrantedItem(item);
        return this;
    }

    public FormBuilder grantedItem(ItemStack stack) {
        form.addGrantedItem(stack);
        return this;
    }

    public FormBuilder grantedItem(Item item, int count) {
        form.addGrantedItem(new ItemStack(item, count));
        return this;
    }

    // ========== 技能 ==========

    /**
     * 自动注册技能并添加到形态
     */
    public FormBuilder skill(String skillPath, int cooldownSeconds) {
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath(
                form.getFormId().getNamespace(),
                form.getFormId().getPath() + "_" + skillPath
        );
        return skill(skillId, cooldownSeconds);
    }

    /**
     * 自动注册技能并添加到形态
     */
    public FormBuilder skill(ResourceLocation skillId, int cooldownSeconds) {
        Component displayName = Component.translatable("skill." + skillId.getNamespace() + "." + skillId.getPath());
        SkillSystem.registerSkill(skillId, displayName, cooldownSeconds);
        form.addSkill(skillId);
        return this;
    }

    /**
     * 直接添加已注册的技能ID
     */
    public FormBuilder skill(ResourceLocation skillId) {
        form.addSkill(skillId);
        return this;
    }

    // ========== 其他 ==========

    public FormBuilder allowsEmptyDriver(boolean allow) {
        form.setAllowsEmptyDriver(allow);
        return this;
    }

    public FormBuilder shouldPause(boolean pause) {
        form.setShouldPause(pause);
        return this;
    }

    public FormBuilder henshinSound(SoundEvent sound) {
        form.setHenshinSound(sound);
        return this;
    }

    public FormBuilder autoCompleteT(int ticks) {
        form.setAutoCompleteTicks(ticks);
        return this;
    }

    public FormBuilder autoCompleteS(float seconds) {
        int ticks = (int) (seconds * 20);
        form.setAutoCompleteTicks(ticks);
        return this;
    }

    // ========== 结束构建 ==========

    /**
     * 结束形态构建，返回父 Builder（仅当通过 RiderBuilder.form() 创建时可用）
     *
     * @throws IllegalStateException 如果独立构建时调用此方法
     */
    public RiderBuilder end() {
        if (parent == null) {
            throw new IllegalStateException(
                    "This FormBuilder was created standalone (via FormBuilder.create). " +
                            "Use .build() to get FormConfig, not .end()."
            );
        }
        parent.addFormBuilder(form.getFormId().getPath(), this);
        return parent;
    }

    /**
     * 完成构建，返回 FormConfig 实例（独立构建时使用）
     */
    public FormConfig build() {
        return form;
    }
}
