package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 理解为管理所有被注册骑士的列表
 */

public class RiderRegistry {
    private static final Map<ResourceLocation, RiderConfig> RIDERS = new HashMap<>();
    private static final Map<ResourceLocation, FormConfig> FORMS = new HashMap<>();

    // 注册骑士配置
    public static void registerRider(RiderConfig config) {
        RIDERS.put(config.getRiderId(), config);
        // 自动注册所有形态
        for (FormConfig form : config.forms.values()) {
            FORMS.put(form.getFormId(), form);
        }
    }

    // 注册形态配置
    public static void registerForm(FormConfig form) {
        FORMS.put(form.getFormId(), form);
    }


    // 获取骑士配置
    public static RiderConfig getRider(ResourceLocation riderId) {
        return RIDERS.get(riderId);
    }

    // 获取形态配置
    public static FormConfig getForm(ResourceLocation formId) {
        return FORMS.get(formId);
    }

    // 获取所有注册的骑士
    public static Collection<RiderConfig> getRegisteredRiders() {
        return RIDERS.values();
    }
}