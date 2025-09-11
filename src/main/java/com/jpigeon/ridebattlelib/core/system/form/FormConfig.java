package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSlotDefinition;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import io.netty.handler.logging.LogLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 形态配置类。
 * 定义变身后的盔甲、属性修饰符、状态效果、技能等。
 * <p>
 * 可通过 RiderConfig.addForm() 添加。
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
    private boolean allowsEmptyDriver = false;
    private boolean shouldPause = false;
    private final List<ResourceLocation> skillIds = new ArrayList<>();

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

    //单独设置
    public void setHelmet(Item helmet) {
        this.helmet = helmet;
    }

    public void setChestplate(Item chestplate) {
        this.chestplate = chestplate;
    }

    public void setLeggings(@Nullable Item leggings) {
        this.leggings = leggings;
    }

    public void setBoots(Item boots) {
        this.boots = boots;
    }

    public void setAllowsEmptyDriver(boolean allow) {
        this.allowsEmptyDriver = allow;
    }

    // 触发方式
    public FormConfig setTriggerType(TriggerType type) {
        this.triggerType = type;
        return this;
    }

    // 添加属性修饰符
    public FormConfig addAttribute(ResourceLocation attributeId, double amount,
                                   AttributeModifier.Operation operation) {
        attributes.add(new AttributeModifier(attributeId, amount, operation));
        attributeIds.add(attributeId);
        return this;
    }
    // 添加生物效果
    public FormConfig addEffect(Holder<MobEffect> effect, int duration,
                                int amplifier, boolean hideParticles) {
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        effectIds.add(effectId);
        boolean visible = !hideParticles;
        effects.add(new MobEffectInstance(effect, duration, amplifier, false, visible));
        return this;
    }
    // 添加必要物品（主驱动器）
    public FormConfig addRequiredItem(ResourceLocation slotId, Item item) {
        requiredItems.put(slotId, item);
        return this;
    }
    // 添加必要物品（副驱动器）
    public FormConfig addAuxRequiredItem(ResourceLocation slotId, Item item) {
        auxRequiredItems.put(slotId, item);
        return this;
    }
    // 给予物品（变身后）
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
    public boolean matchesMainSlots(Map<ResourceLocation, ItemStack> driverItems, RiderConfig config) {
        for (Map.Entry<ResourceLocation, Item> entry : requiredItems.entrySet()) {

            if (requiredItems.isEmpty() && !allowsEmptyDriver) {
                // 检查驱动器是否为空（跳过辅助槽位）
                for (ResourceLocation slotId : config.getSlotDefinitions().keySet()) {
                    ItemStack stack = driverItems.get(slotId);
                    if (stack != null && !stack.isEmpty()) {
                        return true; // 非空即匹配
                    }
                }
                return false; // 所有主槽位都为空
            }

            ResourceLocation slotId = entry.getKey();
            Item requiredItem = entry.getValue();

            DriverSlotDefinition slotDef = config.getSlotDefinition(slotId);
            if (slotDef == null) {
                RideBattleLib.LOGGER.warn("未找到槽位定义: {}", slotId);
                return false;
            }

            ItemStack stack = driverItems.get(slotId);

            // 必需槽位不能为空
            if (slotDef.isRequired() && (stack == null || stack.isEmpty())) {
                return false;
            }

            // 如果形态明确要求某物品，即使槽位非必需，也必须匹配
            if (requiredItem != null && (stack == null || !stack.is(requiredItem))) {
                if (stack != null && Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                    RideBattleLib.LOGGER.debug("槽位 {} 要求物品 {}, 实际为 {}", slotId, requiredItem, stack.getItem());
                }
                return false;
            }
            if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                RideBattleLib.LOGGER.debug("槽位 {} 匹配成功 {}", slotId, stack);
            }
        }
        return true;
    }

    public boolean matchesAuxSlots(Map<ResourceLocation, ItemStack> driverItems, RiderConfig config) {
        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)){
            RideBattleLib.LOGGER.debug("开始匹配辅助槽位...");
        }
        for (Map.Entry<ResourceLocation, Item> entry : auxRequiredItems.entrySet()) {
            ResourceLocation slotId = entry.getKey();
            Item requiredItem = entry.getValue();
            ItemStack stack = driverItems.get(slotId);

            DriverSlotDefinition slotDef = config.getAuxSlotDefinition(slotId);
            if (slotDef == null) {
                RideBattleLib.LOGGER.warn("未找到辅助槽位: {}", slotId);
                return false;
            }

            // 必需槽位不能为空
            if (slotDef.isRequired() && (stack == null || stack.isEmpty())) {
                return false;
            }

            // 如果形态明确要求某物品，即使非必需，也必须匹配
            if (requiredItem != null && (stack == null || !stack.is(requiredItem))) {
                if (stack != null) {
                    if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                        RideBattleLib.LOGGER.debug("辅助槽位 {} 要求物品 {}, 实际为 {}", slotId, requiredItem, stack.getItem());
                    }
                }
                return false;
            }
            if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                RideBattleLib.LOGGER.debug("辅助槽位 {} 匹配成功", slotId);
            }
        }
        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("辅助槽位全部匹配");
        }
        return true;
    }

    public FormConfig addSkill(ResourceLocation skillId) {
        if (!skillIds.contains(skillId)) {
            skillIds.add(skillId);
        }
        return this;
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

    public boolean allowsEmptyDriver() {
        return allowsEmptyDriver;
    }

    public List<ItemStack> getGrantedItems() {
        return Collections.unmodifiableList(grantedItems);
    }

    public boolean shouldPause() {
        return shouldPause;
    }

    public boolean hasAuxRequirements() {
        return !auxRequiredItems.isEmpty();
    }

    public List<ResourceLocation> getSkillIds() {
        return Collections.unmodifiableList(skillIds);
    }

    public ResourceLocation getCurrentSkillId(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        int index = data.getCurrentSkillIndex();
        return skillIds.isEmpty() ? null : skillIds.get(index % skillIds.size());
    }
}
