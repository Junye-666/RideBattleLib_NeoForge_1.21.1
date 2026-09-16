package com.jpigeon.ridebattlelib.common.config;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.config.dynamic.DynamicFormCache;
import com.jpigeon.ridebattlelib.common.util.RiderUtils;
import com.jpigeon.ridebattlelib.server.event.FormOverrideEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.Map;

// ridebattlelib/rider/FormMatchEngine.java
public final class FormMatchEngine {
    private FormMatchEngine() {}

    public static @Nullable ResourceLocation match(
            Player player, RiderConfig config, Map<ResourceLocation, ItemStack> items) {

        FormOverrideEvent override = new FormOverrideEvent(player, items, null);
        NeoForge.EVENT_BUS.post(override);
        if (override.isCanceled()) return RiderUtils.NULL;
        if (override.getOverrideForm() != null) return override.getOverrideForm();

        if (isEmpty(items)) {
            FormConfig base = config.getForms(config.getBaseFormId());
            if (base != null && base.allowsEmptyDriver()) return config.getBaseFormId();
            return RiderUtils.NULL;
        }

        // 必需槽位检查
        for (ResourceLocation slotId : config.getRequiredSlots()) {
            DriverSlotDefinition def = config.getSlotDefinition(slotId);
            if (def == null || !def.isRequired()) continue;
            ItemStack stack = items.get(slotId);
            if (stack == null || stack.isEmpty()) return RiderUtils.NULL;
        }
        for (ResourceLocation slotId : config.getAuxRequiredSlots()) {
            DriverSlotDefinition def = config.getAuxSlotDefinition(slotId);
            if (def == null || !def.isRequired()) continue;
            ItemStack stack = items.get(slotId);
            if (stack == null || stack.isEmpty()) return RiderUtils.NULL;
        }

        // 遍历预设形态
        for (FormConfig form : config.getForms().values()) {
            if (form.matchesMainSlots(items, config)
                    && (form.getAuxRequiredItems().isEmpty()
                    || config.hasAuxDriverEquipped(player)
                    && form.matchesAuxSlots(items, config))) {
                return form.getFormId();
            }
        }

        // 动态形态
        if (config.allowsDynamicForms()) {
            try {
                return DynamicFormCache.getOrCreate(config, items).getFormId();
            } catch (Exception e) {
                RideBattleLib.LOGGER.error("动态形态生成失败", e);
            }
        }
        return RiderUtils.NULL;
    }

    private static boolean isEmpty(Map<ResourceLocation, ItemStack> items) {
        if (items.isEmpty()) return true;
        for (ItemStack s : items.values()) if (!s.isEmpty()) return false;
        return true;
    }
}
