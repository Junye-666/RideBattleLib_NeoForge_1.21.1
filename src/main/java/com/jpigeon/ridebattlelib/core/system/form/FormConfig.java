package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.SlotDefinition;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

/*
  * 骑士形态配置
  * setArmor 盔甲设置
  * addAttribute 添加属性修饰符
  * addEffect 添加效果
  * addRequiredItem 添加必需物品
  * addGrantedItem 添加变身后给予玩家的物品
  * setShouldPause 添加形态变身时是否缓冲(默认false->0帧起手的变身)
 */
public class FormConfig {
    private final ResourceLocation formId;
    private Item helmet = Items.AIR;
    private Item chestplate = Items.AIR;
    private @Nullable Item leggings = Items.AIR;
    private Item boots = Items.AIR;
    private TriggerType triggerType = TriggerType.KEY;
    private final List<AttributeModifier> attributes = new ArrayList<>();
    private final List<MobEffectInstance> effects = new ArrayList<>();
    private final List<ResourceLocation> attributeIds = new ArrayList<>();
    private final List<ResourceLocation> effectIds = new ArrayList<>();
    private final Map<ResourceLocation, Item> requiredItems = new HashMap<>();
    private final Map<ResourceLocation, Item> auxRequiredItems = new HashMap<>();
    private final List<ItemStack> grantedItems = new ArrayList<>();
    private boolean allowsEmptyBelt = false;
    private boolean shouldPause = false;

    public FormConfig(ResourceLocation formId) {
        this.formId = formId;
    }

    //====================Setter方法====================

    public FormConfig setArmor(Item helmet, Item chestplate, @Nullable Item leggings, Item boots) {
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings != null ? leggings : Items.AIR;
        this.boots = boots;
        return this;
    }

    public void setAllowsEmptyBelt(boolean allow) {
        this.allowsEmptyBelt = allow;
    }

    public FormConfig setTriggerType(TriggerType type) {
        this.triggerType = type;
        return this;
    }

    public FormConfig addAttribute(ResourceLocation attributeId, double amount,
                                   AttributeModifier.Operation operation) {
        UUID uuid = UUID.nameUUIDFromBytes(
                (formId + attributeId.toString()).getBytes(StandardCharsets.UTF_8)
        );
        attributes.add(new AttributeModifier(attributeId, amount, operation));
        attributeIds.add(attributeId);
        return this;
    }

    public FormConfig addEffect(Holder<MobEffect> effect, int duration,
                                int amplifier, boolean hideParticles) {
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        effectIds.add(effectId);
        effects.add(new MobEffectInstance(effect, duration, amplifier, false, hideParticles));
        return this;
    }

    public FormConfig addRequiredItem(ResourceLocation slotId, Item item) {
        requiredItems.put(slotId, item);
        return this;
    }

    public FormConfig addAuxRequiredItem(ResourceLocation slotId, Item item) {
        auxRequiredItems.put(slotId, item);
        return this;
    }

    public FormConfig addGrantedItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            grantedItems.add(stack.copy());
        }
        return this;
    }

    public void setShouldPause(boolean pause) {
        this.shouldPause = pause;
    }

    // 匹配验证
    public boolean matchesMainSlots(Map<ResourceLocation, ItemStack> beltItems, RiderConfig config) {
        for (Map.Entry<ResourceLocation, Item> entry : requiredItems.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            Item requiredItem = entry.getValue();
            ItemStack stack = beltItems.get(slotId);

            SlotDefinition slotDef = config.getSlotDefinition(slotId);
            if (slotDef == null) {
                RideBattleLib.LOGGER.warn("未找到槽位定义: {}", slotId);
                return false;
            }

            // 必需槽位不能为空
            if (slotDef.isRequired() && (stack == null || stack.isEmpty())) {
                RideBattleLib.LOGGER.warn("必需槽位 {} 为空", slotId);
                return false;
            }

            // 如果形态明确要求某物品，即使槽位非必需，也必须匹配
            if (requiredItem != null && (stack == null || !stack.is(requiredItem))) {
                if (stack != null) {
                    RideBattleLib.LOGGER.warn("槽位 {} 要求物品 {}, 实际为 {}", slotId, requiredItem, stack.getItem());
                }
                return false;
            }

            RideBattleLib.LOGGER.debug("槽位 {} 匹配成功 {}", slotId, stack);
        }
        return true;
    }

    public boolean matchesAuxSlots(Map<ResourceLocation, ItemStack> beltItems, RiderConfig config) {
        RideBattleLib.LOGGER.debug("开始匹配辅助槽位...");
        for (Map.Entry<ResourceLocation, Item> entry : auxRequiredItems.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            Item requiredItem = entry.getValue();
            ItemStack stack = beltItems.get(slotId);

            SlotDefinition slotDef = config.getAuxSlotDefinition(slotId);
            if (slotDef == null) {
                RideBattleLib.LOGGER.warn("未找到辅助槽位定义: {}", slotId);
                return false;
            }

            // 必需槽位不能为空
            if (slotDef.isRequired() && (stack == null || stack.isEmpty())) {
                RideBattleLib.LOGGER.warn("必需辅助槽位 {} 为空", slotId);
                return false;
            }

            // 如果形态明确要求某物品，即使非必需，也必须匹配
            if (requiredItem != null && (stack == null || !stack.is(requiredItem))) {
                if (stack != null) {
                    RideBattleLib.LOGGER.warn("辅助槽位 {} 要求物品 {}, 实际为 {}", slotId, requiredItem, stack.getItem());
                }
                return false;
            }

            RideBattleLib.LOGGER.debug("辅助槽位 {} 匹配成功", slotId);
        }
        RideBattleLib.LOGGER.debug("辅助槽位全部匹配");
        return true;
    }

    //====================检查方法====================
    public boolean hasAuxRequirements() {
        return !auxRequiredItems.isEmpty();
    }

    //====================Getter方法====================
    public ResourceLocation getFormId() {
        return formId;
    }

    public Item getHelmet() {
        return helmet;
    }

    public Item getChestplate() {
        return chestplate;
    }

    public @Nullable Item getLeggings() {
        return leggings;
    }

    public Item getBoots() {
        return boots;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public List<AttributeModifier> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public List<MobEffectInstance> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public List<ResourceLocation> getAttributeIds() {
        return Collections.unmodifiableList(attributeIds);
    }

    public List<ResourceLocation> getEffectIds() {
        return Collections.unmodifiableList(effectIds);
    }

    public Map<ResourceLocation, Item> getRequiredItems() {
        return Collections.unmodifiableMap(requiredItems);
    }

    public Map<ResourceLocation, Item> getAuxRequiredItems() {
        return Collections.unmodifiableMap(auxRequiredItems);
    }

    public boolean allowsEmptyBelt() {
        return allowsEmptyBelt;
    }

    public List<ItemStack> getGrantedItems() {
        return Collections.unmodifiableList(grantedItems);
    }

    public boolean shouldPause() {
        return shouldPause;
    }
}
