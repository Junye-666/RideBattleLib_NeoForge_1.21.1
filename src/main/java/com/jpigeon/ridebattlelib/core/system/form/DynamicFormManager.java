package com.jpigeon.ridebattlelib.core.system.form;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import io.netty.handler.logging.LogLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class DynamicFormManager {
    private static final Map<ResourceLocation, FormConfig> DYNAMIC_FORMS = new HashMap<>();
    private static final Map<ResourceLocation, Long> LAST_USED = new HashMap<>();
    private static final long UNLOAD_DELAY = 10 * 60 * 1000; // 10分钟未使用则卸载

    public static void cleanupUnusedForms() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ResourceLocation, FormConfig>> it = DYNAMIC_FORMS.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<ResourceLocation, FormConfig> entry = it.next();
            long lastUsed = LAST_USED.getOrDefault(entry.getKey(), 0L);

            if (now - lastUsed > UNLOAD_DELAY) {
                if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                    RideBattleLib.LOGGER.debug("卸载动态形态: {}", entry.getKey());
                }
                it.remove();
                LAST_USED.remove(entry.getKey());
            }
        }
    }

    public static FormConfig getOrCreateDynamicForm(Player player, RiderConfig config, Map<ResourceLocation, ItemStack> driverItems) {
        // 1. 生成formId
        ResourceLocation formId = generateFormId(config.getRiderId(), driverItems);

        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("创建动态形态: {}", formId);
            RideBattleLib.LOGGER.debug("槽位内容: {}",
                    driverItems.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue().getItem())
                            .collect(Collectors.joining(", ")));
        }

        // 2. 检查缓存
        if (DYNAMIC_FORMS.containsKey(formId)) {
            LAST_USED.put(formId, System.currentTimeMillis());
            return DYNAMIC_FORMS.get(formId);
        }

        // 3. 创建新形态
        FormConfig form = new DynamicFormConfig(formId, driverItems, config);
        FormConfig baseForm = config.getForms(config.getBaseFormId());
        if (baseForm != null) {
            form.setTriggerType(baseForm.getTriggerType());
            form.setShouldPause(baseForm.shouldPause()); // 新增这行
        } else {
            form.setTriggerType(TriggerType.KEY);
            // shouldPause 默认为 false
        }

        DYNAMIC_FORMS.put(formId, form);
        LAST_USED.put(formId, System.currentTimeMillis());

        // 4. 自动清理任务 - 优化执行频率
        MinecraftServer server = player.getServer();
        if (server != null && server.getTickCount() % 6000 == 0) { // 每5分钟检查一次
            server.execute(DynamicFormManager::cleanupUnusedForms);
        }

        return form;
    }

    private static ResourceLocation generateFormId(
            ResourceLocation riderId,
            Map<ResourceLocation, ItemStack> driverItems
    ) {
        // 提取骑士基础ID (e.g. "kamen_rider_build" -> "build")
        String baseId = riderId.getPath().replace("kamen_rider_", "");

        // 收集物品ID并去重
        Set<String> itemParts = new LinkedHashSet<>();
        for (ItemStack stack : driverItems.values()) {
            if (!stack.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String trimmedId = trimCommonSuffix(itemId.getPath());
                itemParts.add(trimmedId);
            }
        }

        // 组合formId (e.g. "build_tank_panda")
        String formPath = baseId + "_" + String.join("_", itemParts);
        return ResourceLocation.fromNamespaceAndPath(riderId.getNamespace(), formPath);
    }

    private static String trimCommonSuffix(String itemId) {
        // 移除常见后缀 (可扩展)
        String[] suffixes = {"_full_bottle", "_medal", "_core"};
        for (String suffix : suffixes) {
            if (itemId.endsWith(suffix)) {
                return itemId.substring(0, itemId.length() - suffix.length());
            }
        }
        return itemId;
    }

    public static FormConfig getDynamicForm(ResourceLocation formId) {
        return DYNAMIC_FORMS.get(formId);
    }
}
