package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 理解为管理所有被注册骑士的列表
 */
public class RiderRegistry {
    private static final Map<ResourceLocation, RiderConfig> RIDERS = new HashMap<>();
    private static final Map<ResourceLocation, FormConfig> FORMS = new HashMap<>();
    // 添加映射：形态ID -> 所属骑士ID列表（一个形态可能被多个骑士使用）
    private static final Map<ResourceLocation, Set<ResourceLocation>> FORM_TO_RIDERS = new HashMap<>();

    public static void registerRider(RiderConfig config) {
        RIDERS.put(config.getRiderId(), config);

        // 注册所有形态，并建立形态到骑士的映射
        for (FormConfig form : config.forms.values()) {
            registerFormForRider(form, config.getRiderId());
        }
    }

    // 为特定骑士注册形态
    private static void registerFormForRider(FormConfig form, ResourceLocation riderId) {
        ResourceLocation formId = form.getFormId();
        FORMS.put(formId, form);

        FORM_TO_RIDERS.computeIfAbsent(formId, k -> new HashSet<>()).add(riderId);

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("为骑士 {} 注册形态 {} (总注册数: {})",
                    riderId, formId, FORM_TO_RIDERS.get(formId).size());
        }
    }

    // 获取形态配置（优先检查玩家当前骑士）
    public static FormConfig getForm(Player player, ResourceLocation formId) {
        if (player == null) {
            return getForm(formId); // 降级到基础方法
        }

        RiderConfig activeConfig = RiderConfig.findActiveDriverConfig(player);
        if (activeConfig != null) {
            // 首先从玩家当前骑士配置中查找
            FormConfig riderForm = activeConfig.getForms().get(formId);
            if (riderForm != null) {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("从玩家 {} 的骑士 {} 获取形态 {}",
                            player.getName().getString(), activeConfig.getRiderId(), formId);
                }
                return riderForm;
            }
        }

        // 如果没有特定骑士的配置，则使用通用版本
        return getForm(formId);
    }

    // 原有的基础方法（向后兼容）
    public static FormConfig getForm(ResourceLocation formId) {
        return FORMS.get(formId);
    }

    // 检查形态是否属于特定骑士
    public static boolean isFormForRider(ResourceLocation formId, ResourceLocation riderId) {
        Set<ResourceLocation> riderSet = FORM_TO_RIDERS.get(formId);
        return riderSet != null && riderSet.contains(riderId);
    }

    // 获取形态的所有拥有者骑士
    public static Set<ResourceLocation> getFormOwners(ResourceLocation formId) {
        return FORM_TO_RIDERS.getOrDefault(formId, Collections.emptySet());
    }

    // 获取骑士配置
    public static RiderConfig getRider(ResourceLocation riderId) {
        return RIDERS.get(riderId);
    }

    // 获取所有注册的骑士
    public static Collection<RiderConfig> getRegisteredRiders() {
        return RIDERS.values();
    }

    // 获取所有已注册的形态
    public static Collection<FormConfig> getAllForms() {
        return FORMS.values();
    }
}