package com.jpigeon.ridebattlelib.common.config.dynamic;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.config.TriggerType;
import com.jpigeon.ridebattlelib.common.util.RiderUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicFormCache {
    private DynamicFormCache() {
    }

    private static final Map<ResourceLocation, FormConfig> DYNAMIC_FORMS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Long> LAST_USED_TICK = new ConcurrentHashMap<>();

    /**
     * 10 分钟 = 12000 ticks
     */
    private static final long UNLOAD_DELAY_TICKS = 10L * 60L * 20L;
    /**
     * 每 5 分钟跑一次清理
     */
    private static final long CLEANUP_INTERVAL_TICKS = 5L * 60L * 20L;

    // ===== 主入口 =====

    public static FormConfig getOrCreate(RiderConfig config,
                                         Map<ResourceLocation, ItemStack> items,
                                         long nowTick) {
        ResourceLocation formId = DynamicFormGenerator.generateId(config.getRiderId(), items);

        FormConfig cached = DYNAMIC_FORMS.get(formId);
        if (cached != null) {
            LAST_USED_TICK.put(formId, nowTick);
            return cached;
        }

        FormConfig form = DynamicFormGenerator.generate(formId, items, config);
        FormConfig base = config.getForms(config.getBaseFormId());
        if (base != null) {
            form.setTriggerType(base.getTriggerType());
            form.setShouldPause(base.shouldPause());
        } else {
            form.setTriggerType(TriggerType.KEY);
        }

        DYNAMIC_FORMS.put(formId, form);
        LAST_USED_TICK.put(formId, nowTick);
        return form;
    }

    public static @Nullable FormConfig get(ResourceLocation formId) {
        if (formId == null || formId.equals(RiderUtils.NULL)) return null;
        return DYNAMIC_FORMS.get(formId);
    }

    // ===== 清理 =====

    public static void cleanup(long nowTick) {
        Iterator<Map.Entry<ResourceLocation, FormConfig>> it = DYNAMIC_FORMS.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            var entry = it.next();
            long last = LAST_USED_TICK.getOrDefault(entry.getKey(), 0L);
            if (nowTick - last > UNLOAD_DELAY_TICKS) {
                it.remove();
                LAST_USED_TICK.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0 && Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("清理 {} 个未使用的动态形态", removed);
        }
    }

    @EventBusSubscriber(modid = RideBattleLib.MODID)
    public static final class CleanupHandler {
        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post e) {
            long tick = e.getServer().getTickCount();
            if (tick % CLEANUP_INTERVAL_TICKS != 0) return;
            cleanup(tick);
        }
    }
}