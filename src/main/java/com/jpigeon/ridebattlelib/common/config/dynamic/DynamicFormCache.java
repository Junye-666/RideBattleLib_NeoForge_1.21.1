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
    private static final Map<ResourceLocation, Long> LAST_USED = new ConcurrentHashMap<>();
    private static final long UNLOAD_DELAY = 10 * 60 * 1000L;

    // ========== 主入口：拿/建 ==========
    public static FormConfig getOrCreate(RiderConfig config, Map<ResourceLocation, ItemStack> items) {
        ResourceLocation formId = DynamicFormGenerator.generateId(config.getRiderId(), items);

        FormConfig cached = DYNAMIC_FORMS.get(formId);
        if (cached != null) {
            LAST_USED.put(formId, System.currentTimeMillis());
            return cached;
        }

        FormConfig form = DynamicFormGenerator.generate(formId, items, config);

        // 从 baseForm 继承 triggerType / shouldPause
        FormConfig base = config.getForms(config.getBaseFormId());
        if (base != null) {
            form.setTriggerType(base.getTriggerType());
            form.setShouldPause(base.shouldPause());
        } else {
            form.setTriggerType(TriggerType.KEY);
        }

        DYNAMIC_FORMS.put(formId, form);
        LAST_USED.put(formId, System.currentTimeMillis());
        return form;
    }

    public static @Nullable FormConfig get(ResourceLocation formId) {
        if (formId == null || formId.equals(RiderUtils.NULL)) return null;
        return DYNAMIC_FORMS.get(formId);
    }

    // ========== 清理 ==========
    public static void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ResourceLocation, FormConfig>> it = DYNAMIC_FORMS.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            var entry = it.next();
            long last = LAST_USED.getOrDefault(entry.getKey(), 0L);
            if (now - last > UNLOAD_DELAY) {
                it.remove();
                LAST_USED.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0 && Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("清理 {} 个未使用的动态形态", removed);
        }
    }

    // ========== 事件订阅（原内部类剥离） ==========
    @EventBusSubscriber(modid = RideBattleLib.MODID)
    public static final class CleanupHandler {
        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post e) {
            if (e.getServer().getTickCount() % 6000 != 0) return;
            cleanup();
        }
    }
}