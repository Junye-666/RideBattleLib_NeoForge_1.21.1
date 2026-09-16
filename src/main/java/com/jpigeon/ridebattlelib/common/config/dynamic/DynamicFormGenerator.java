package com.jpigeon.ridebattlelib.common.config.dynamic;

import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class DynamicFormGenerator {
    private DynamicFormGenerator() {
    }

    /**
     * 从 driver 快照生成一个动态形态数据对象（不做缓存）。
     */
    public static DynamicFormConfig generate(
            ResourceLocation formId,
            Map<ResourceLocation, ItemStack> snapshot,
            RiderConfig config) {

        DynamicFormConfig form = new DynamicFormConfig(formId, snapshot);

        // 1) 复制骑士基础属性/效果
        for (AttributeModifier attr : config.getBaseAttributes()) {
            form.addDynamicAttribute(new AttributeModifier(attr.id(), attr.amount(), attr.operation()));
        }
        for (MobEffectInstance eff : config.getBaseEffects()) {
            form.addDynamicEffect(new MobEffectInstance(eff));
        }

        // 2) 遍历快照物品，映射盔甲、效果、授予
        Set<EquipmentSlot> usedSlots = new HashSet<>();
        for (var e : snapshot.entrySet()) {
            ItemStack stack = e.getValue();
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            // 2.1 盔甲槽位
            EquipmentSlot armorSlot = determineArmorSlot(e.getKey(), item);
            if (armorSlot != null) {
                usedSlots.add(armorSlot);
                Item armorItem = DynamicMappingRegistry.getArmorForItem(item, armorSlot);
                if (armorItem != Items.AIR) {
                    form.applyArmorSlot(armorSlot, armorItem);
                }
            }

            // 2.2 效果
            for (MobEffectInstance eff : DynamicMappingRegistry.getEffectsForItem(item)) {
                form.addDynamicEffect(new MobEffectInstance(eff));
            }

            // 2.3 授予物品
            for (ItemStack grant : DynamicMappingRegistry.getGrantedItemsForItem(item)) {
                form.addDynamicGranted(grant.copy());
            }
        }

        // 3) 未使用槽位填底衣
        Map<EquipmentSlot, Item> undersuit = DynamicMappingRegistry.getRiderUndersuit(config.getRiderId());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (usedSlots.contains(slot)) continue;
            Item u = undersuit.get(slot);
            if (u != null && u != Items.AIR) {
                form.applyArmorSlot(slot, u);
            }
        }

        return form;
    }

    // ========== 自动判定盔甲槽位 ==========
    private static @Nullable EquipmentSlot determineArmorSlot(ResourceLocation slotId, Item item) {
        // 1) 槽位名模式
        EquipmentSlot byName = DynamicMappingRegistry.matchSlotPattern(slotId.getPath());
        if (byName != null) return byName;

        // 2) 显式映射表
        Map<EquipmentSlot, Item> explicit = DynamicMappingRegistry.peekItemArmor(item);
        if (explicit != null && !explicit.isEmpty()) {
            return explicit.keySet().iterator().next();
        }

        // 3) 物品类型
        if (item instanceof ArmorItem a) return a.getEquipmentSlot();

        // 4) 物品 id 推断
        return DynamicMappingRegistry.getAutoArmorSlot(item);
    }

    // ========== 动态形态 ID 生成 ==========
    public static ResourceLocation generateId(ResourceLocation riderId,
                                              Map<ResourceLocation, ItemStack> items) {
        String baseId = riderId.getPath().replace("kamen_rider_", "");
        Set<String> itemPaths = new LinkedHashSet<>();
        for (ItemStack stack : items.values()) {
            if (!stack.isEmpty()) {
                itemPaths.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
            }
        }
        if (itemPaths.size() <= 1) {
            String suffix = itemPaths.isEmpty() ? "empty" : itemPaths.iterator().next();
            return ResourceLocation.fromNamespaceAndPath(riderId.getNamespace(), baseId + "_" + suffix);
        }

        String common = findLongestCommonSuffix(itemPaths);
        if (common.length() >= 2) {
            Set<String> trimmed = new LinkedHashSet<>();
            for (String p : itemPaths) {
                trimmed.add(p.endsWith(common) ? p.substring(0, p.length() - common.length()) : p);
            }
            itemPaths = trimmed;
        }
        return ResourceLocation.fromNamespaceAndPath(
                riderId.getNamespace(), baseId + "_" + String.join("_", itemPaths));
    }

    /**
     * 找共同后缀。修复原 bug：不再用"第一个字符串"当基准（可能是无下划线的）。
     * 改为"第一个含下划线的字符串"作基准。
     */
    private static String findLongestCommonSuffix(Set<String> strings) {
        if (strings.isEmpty()) return "";
        String base = null;
        for (String s : strings) {
            if (s.indexOf('_') >= 0) {
                base = s;
                break;
            }
        }
        if (base == null) return "";

        String best = "";
        for (int i = base.indexOf('_'); i < base.length(); i++) {
            String suffix = base.substring(i);
            if (!suffix.startsWith("_") || suffix.length() < 2) continue;

            boolean all = true;
            for (String s : strings) {
                if (!s.endsWith(suffix)) {
                    all = false;
                    break;
                }
            }
            if (all && suffix.length() > best.length()) best = suffix;
        }
        return best;
    }
}