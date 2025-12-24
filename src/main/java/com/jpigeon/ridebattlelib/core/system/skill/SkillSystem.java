package com.jpigeon.ridebattlelib.core.system.skill;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.event.RotateSkillEvent;
import com.jpigeon.ridebattlelib.core.system.event.SkillEvent;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkillSystem {
    // 只保留技能名称注册
    private static final Map<ResourceLocation, Component> SKILL_DISPLAY_NAMES = new HashMap<>();

    // 技能冷却时间配置（毫秒）
    private static final Map<ResourceLocation, Long> SKILL_COOLDOWN_MAP = new HashMap<>();

    // 玩家技能冷却记录
    private static final Map<UUID, Map<ResourceLocation, Long>> PLAYER_SKILL_COOLDOWNS = new ConcurrentHashMap<>();

    // ==================== 注册方法 ====================

    public static void registerSkill(ResourceLocation skillId, Component displayName, int cooldownSeconds) {
        registerSkillName(skillId, displayName);
        registerSkillCooldown(skillId, cooldownSeconds);
    }

    private static void registerSkillName(ResourceLocation skillId, Component displayName) {
        SKILL_DISPLAY_NAMES.put(skillId, displayName);
    }

    private static void registerSkillCooldown(ResourceLocation skillId, int cooldownSeconds) {
        SKILL_COOLDOWN_MAP.put(skillId, (long) cooldownSeconds * 1000);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取玩家当前形态的技能列表
     * @param player 玩家
     * @return 技能ID列表，无技能则返回空列表
     */
    public static List<ResourceLocation> getCurrentFormSkills(Player player) {
        FormConfig formConfig = getActiveFormConfig(player);
        return formConfig != null ? formConfig.getSkillIds() : Collections.emptyList();
    }

    /**
     * 获取玩家当前形态配置
     * @param player 玩家
     * @return 当前形态配置，未找到则返回null
     */
    @Nullable
    public static FormConfig getActiveFormConfig(Player player) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return null;

        // 优先从玩家当前骑士获取形态配置
        FormConfig form = RiderRegistry.getForm(player, data.formId());
        if (form == null) {
            // 尝试动态形态
            form = DynamicFormConfig.getDynamicForm(data.formId());
        }

        if (form == null && Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("未找到玩家 {} 的形态配置: {}",
                    player.getName().getString(), data.formId());
        }

        return form;
    }

    /**
     * 获取当前选中的技能ID
     * @param player 玩家
     * @return 当前技能ID，无技能则返回null
     */
    @Nullable
    public static ResourceLocation getCurrentSkillId(Player player) {
        FormConfig formConfig = getActiveFormConfig(player);
        if (formConfig == null) return null;

        return formConfig.getCurrentSkillId(player);
    }

    // ==================== 冷却管理 ====================

    public static int getSkillCooldown(ResourceLocation skillId) {
        Long cooldownMs = SKILL_COOLDOWN_MAP.get(skillId);
        return cooldownMs != null ? (int)(cooldownMs / 1000) : 0;
    }

    public static boolean isSkillOnCooldown(Player player, ResourceLocation skillId) {
        Map<ResourceLocation, Long> playerCooldowns = PLAYER_SKILL_COOLDOWNS.get(player.getUUID());
        if (playerCooldowns == null) return false;

        Long cooldownEnd = playerCooldowns.get(skillId);
        if (cooldownEnd == null) return false;

        return System.currentTimeMillis() < cooldownEnd;
    }

    public static int getSkillRemainingCooldown(Player player, ResourceLocation skillId) {
        Map<ResourceLocation, Long> playerCooldowns = PLAYER_SKILL_COOLDOWNS.get(player.getUUID());
        if (playerCooldowns == null) return 0;

        Long cooldownEnd = playerCooldowns.get(skillId);
        if (cooldownEnd == null) return 0;

        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? (int)((remaining + 999) / 1000) : 0;
    }

    public static void startSkillCooldown(Player player, ResourceLocation skillId) {
        Long cooldownMs = SKILL_COOLDOWN_MAP.get(skillId);
        if (cooldownMs == null || cooldownMs <= 0) return;

        PLAYER_SKILL_COOLDOWNS
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(skillId, System.currentTimeMillis() + cooldownMs);

        if (Config.DEBUG_MODE.get()) {
            int cooldownSeconds = (int)(cooldownMs / 1000);
            RideBattleLib.LOGGER.debug("为玩家 {} 的技能 {} 设置冷却: {}秒",
                    player.getName().getString(), skillId, cooldownSeconds);
        }
    }

    public static void clearSkillCooldown(Player player, ResourceLocation skillId) {
        Map<ResourceLocation, Long> playerCooldowns = PLAYER_SKILL_COOLDOWNS.get(player.getUUID());
        if (playerCooldowns != null) {
            playerCooldowns.remove(skillId);
            if (playerCooldowns.isEmpty()) {
                PLAYER_SKILL_COOLDOWNS.remove(player.getUUID());
            }
        }
    }

    public static void clearAllSkillCooldowns(Player player) {
        PLAYER_SKILL_COOLDOWNS.remove(player.getUUID());

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("清除玩家 {} 的所有技能冷却",
                    player.getName().getString());
        }
    }

    public static Component getDisplayName(ResourceLocation skillId) {
        return SKILL_DISPLAY_NAMES.getOrDefault(skillId,
                Component.literal(skillId.toString()));
    }

    // ==================== 技能触发 ====================

    /**
     * 触发当前选中的技能
     */
    public static void triggerCurrentSkill(Player player) {
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("尝试触发当前技能");
        }

        if (!HenshinSystem.INSTANCE.isTransformed(player)) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("触发技能失败: 玩家未变身");
            }
            return;
        }

        ResourceLocation skillId = getCurrentSkillId(player);
        if (skillId == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("触发技能失败: 无当前技能");
            }
            return;
        }

        // 检查技能冷却
        if (isSkillOnCooldown(player, skillId)) {
            int remaining = getSkillRemainingCooldown(player, skillId);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.literal("技能冷却中，剩余时间: " + remaining + "秒")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
            return;
        }

        // 获取当前形态ID
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("触发技能失败: 无变身数据");
            }
            return;
        }

        // 触发技能事件
        if (triggerSkillEvent(player, data.formId(), skillId, SkillEvent.SkillTriggerType.SYSTEM)) {
            // 技能成功触发后开始冷却
            startSkillCooldown(player, skillId);

            if (Config.DEBUG_MODE.get()) {
                int cooldown = getSkillCooldown(skillId);
                RideBattleLib.LOGGER.debug("技能触发成功: {} (冷却{}秒)", skillId, cooldown);
            }
        }
    }

    /**
     * 触发指定技能（简化版本）
     * @param player 玩家
     * @param skillId 技能ID
     * @return 是否成功触发
     */
    public static boolean triggerSkill(Player player, ResourceLocation skillId) {
        return triggerSkill(player, skillId, SkillEvent.SkillTriggerType.OTHER);
    }

    /**
     * 触发指定技能
     * @param player 玩家
     * @param skillId 技能ID
     * @param type 触发类型
     * @return 是否成功触发
     */
    public static boolean triggerSkill(Player player, ResourceLocation skillId, SkillEvent.SkillTriggerType type) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)) return false;

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return false;

        return triggerSkillEvent(player, data.formId(), skillId, type);
    }

    /**
     * 触发指定形态的技能
     * @param player 玩家
     * @param formId 形态ID
     * @param skillId 技能ID
     * @param type 触发类型
     * @return 是否成功触发
     */
    public static boolean triggerSkill(Player player, ResourceLocation formId,
                                       ResourceLocation skillId, SkillEvent.SkillTriggerType type) {
        if (skillId == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("技能ID为空，无法触发");
            }
            return false;
        }

        // 首先检查技能冷却（这是最重要的修复点）
        if (isSkillOnCooldown(player, skillId)) {
            int remaining = getSkillRemainingCooldown(player, skillId);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.literal("技能冷却中，剩余时间: " + remaining + "秒")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }

            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("技能冷却中: {} (剩余{}秒)", skillId, remaining);
            }
            return false;
        }

        // 只触发事件，不执行具体逻辑
        if (triggerSkillEvent(player, formId, skillId, type)) {
            // 技能成功触发后开始冷却
            startSkillCooldown(player, skillId);
            return true;
        }

        return false;
    }

    /**
     * 触发技能事件（核心方法，只负责事件分发）
     */
    public static boolean triggerSkillEvent(Player player, ResourceLocation formId,
                                            ResourceLocation skillId, SkillEvent.SkillTriggerType type) {
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("触发技能事件: 玩家={}, 形态={}, 技能={}, 类型={}",
                    player.getName().getString(), formId, skillId, type);
        }

        // 触发Pre事件（可取消）
        SkillEvent.Pre preEvent = new SkillEvent.Pre(player, formId, skillId, type);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("技能被取消: {}", skillId);
            }
            return false;
        }

        // 触发Post事件（实际执行逻辑的地方）
        NeoForge.EVENT_BUS.post(new SkillEvent.Post(player, formId, skillId, type));
        return true;
    }

    // ==================== 技能轮转 ====================

    /**
     * 轮转当前形态的技能
     */
    public static void rotateSkill(Player player) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)) return;

        RotateSkillEvent event = new RotateSkillEvent(player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("尝试轮转技能: 玩家={}", player.getName().getString());
        }

        FormConfig formConfig = getActiveFormConfig(player);
        if (formConfig == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("轮转技能失败: 未找到形态配置");
            }
            return;
        }

        RiderData riderData = player.getData(RiderAttachments.RIDER_DATA);
        int currentIndex = riderData.getCurrentSkillIndex();
        int skillCount = formConfig.getSkillIds().size();

        if (skillCount > 0) {
            // 循环切换技能
            int newIndex = (currentIndex + 1) % skillCount;
            riderData.setCurrentSkillIndex(newIndex);

            // 获取新技能的ID
            ResourceLocation newSkill = formConfig.getSkillIds().get(newIndex);
            Component displayName = getDisplayName(newSkill);

            // 显示冷却信息
            int cooldown = getSkillCooldown(newSkill);
            if (cooldown > 0 && player instanceof ServerPlayer serverPlayer && Config.DEVELOPER_MODE.get()) {
                serverPlayer.displayClientMessage(
                        Component.literal("技能冷却: " + cooldown + "秒")
                                .withStyle(ChatFormatting.GRAY),
                        false
                );
            }

            if (player instanceof ServerPlayer serverPlayer) {
                // 显示切换提示
                serverPlayer.displayClientMessage(displayName, true);

                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("轮转技能完成: 玩家={}, 新技能={}, 索引={}/{}",
                            player.getName().getString(), newSkill, newIndex + 1, skillCount);
                }
            }
        } else if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("轮转技能失败: 形态无技能");
        }
    }

    // ==================== 清理方法 ====================

    public static void clearPlayerCooldowns(UUID playerId) {
        PLAYER_SKILL_COOLDOWNS.remove(playerId);

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("清除玩家 {} 的技能冷却", playerId);
        }
    }
}