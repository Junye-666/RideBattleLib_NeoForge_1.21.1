package com.jpigeon.ridebattlelib.common.registry;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.config.dynamic.DynamicFormCache;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.system.SkillSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 理解为管理所有被注册骑士的列表
 */
public class RiderRegistry {
    private static final Map<ResourceLocation, RiderConfig> RIDERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, FormConfig> FORMS = new ConcurrentHashMap<>();
    // 添加映射：形态ID -> 所属骑士ID列表（一个形态可能被多个骑士使用）
    private static final Map<ResourceLocation, Set<ResourceLocation>> FORM_TO_RIDERS = new ConcurrentHashMap<>();

    private static final Map<Item, List<RiderConfig>> DRIVER_ITEM_INDEX = new ConcurrentHashMap<>();

    public static void registerRider(RiderConfig config) {
        RIDERS.put(config.getRiderId(), config);

        Item driverItem = config.getDriverItem();
        if (driverItem != null && driverItem != Items.AIR) {
            DRIVER_ITEM_INDEX
                    .computeIfAbsent(driverItem, k -> new CopyOnWriteArrayList<>())
                    .add(config);
        }

        RiderArmorRegistry.registerRiderArmor(config);

        for (FormConfig form : config.getForms().values()) {
            registerFormForRider(form, config.getRiderId());
            flushPendingSkills(form);   // ← 新增
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
            return getForm(formId);
        }

        ResourceLocation activeRider = player.level().isClientSide()
                ? ClientTransformedCache.getRiderId(player.getUUID())
                : activeRiderId(player);
        if (activeRider != null) {
            Set<ResourceLocation> owners = FORM_TO_RIDERS.get(formId);
            if (owners != null && owners.contains(activeRider)) {
                RiderConfig config = RIDERS.get(activeRider);
                if (config != null) {
                    FormConfig f = config.getForms(formId);
                    if (f != null) return f;
                }
            }
        }

        return getForm(formId);
    }

    /**
     * 将形态声明的 pending 技能注册到 SkillSystem。
     * 如果同一个技能被多个形态声明（不同冷却），后注册的会覆盖先注册的 —— 这是预期行为，
     * 使用者应该保证同一技能 ID 的冷却时间一致。
     */
    private static void flushPendingSkills(FormConfig form) {
        for (Map.Entry<ResourceLocation, Integer> e : form.getPendingSkillCooldowns().entrySet()) {
            ResourceLocation skillId = e.getKey();
            Component name = form.getPendingSkillNames()
                    .getOrDefault(skillId, Component.literal(skillId.toString()));
            SkillSystem.registerSkill(skillId, name, e.getValue());
        }
    }

    private static @Nullable ResourceLocation activeRiderId(Player player) {
        HenshinSessionData session = HenshinUtils.getSessionData(player);
        return session != null ? session.riderId() : null;
    }

    /**
     * 基础查询（向后兼容）：先查静态表，再查动态缓存。
     * <p>
     * 若调用方有 Player 上下文且希望"优先玩家当前骑士的 form"，用
     * {@link #getForm(Player, ResourceLocation)}。
     */
    public static FormConfig getForm(ResourceLocation formId) {
        if (formId == null) return null;
        FormConfig f = FORMS.get(formId);
        if (f != null) return f;
        return DynamicFormCache.get(formId);
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

    public static List<RiderConfig> getRidersByDriver(Item item) {
        return DRIVER_ITEM_INDEX.getOrDefault(item, Collections.emptyList());
    }

    // 获取所有注册的骑士
    public static Collection<RiderConfig> getRegisteredRiders() {
        return Collections.unmodifiableCollection(RIDERS.values()); // 防止修改
    }

    // 获取所有已注册的形态
    public static Collection<FormConfig> getAllForms() {
        return FORMS.values();
    }
}
