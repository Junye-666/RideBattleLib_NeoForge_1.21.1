package com.jpigeon.ridebattlelib.core.system.skill;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.event.SkillEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillSystem {
    // 只保留技能名称注册
    private static final Map<ResourceLocation, Component> SKILL_DISPLAY_NAMES = new HashMap<>();

    // 技能冷却时间配置（毫秒）
    private static final Map<ResourceLocation, Long> SKILL_COOLDOWN_MAP = new HashMap<>();

    // 玩家技能冷却记录
    private static final Map<UUID, Map<ResourceLocation, Long>> PLAYER_SKILL_COOLDOWNS = new ConcurrentHashMap<>();

    public static void registerSkill(ResourceLocation skillId, Component displayName, int cooldownSeconds) {
        registerSkillName(skillId, displayName);
        registerSkillCooldown(skillId, cooldownSeconds);
    }

    // 注册技能显示名称
    private static void registerSkillName(ResourceLocation skillId, Component displayName) {
        SKILL_DISPLAY_NAMES.put(skillId, displayName);
    }

    // 注册技能冷却时间（单位：秒）
    private static void registerSkillCooldown(ResourceLocation skillId, int cooldownSeconds) {
        SKILL_COOLDOWN_MAP.put(skillId, cooldownSeconds * 1000L);
    }

    // 获取技能冷却时间（秒）
    public static int getSkillCooldown(ResourceLocation skillId) {
        Long cooldownMs = SKILL_COOLDOWN_MAP.get(skillId);
        return cooldownMs != null ? (int)(cooldownMs / 1000) : 0;
    }

    // 检查技能是否在冷却中
    public static boolean isSkillOnCooldown(Player player, ResourceLocation skillId) {
        Map<ResourceLocation, Long> playerCooldowns = PLAYER_SKILL_COOLDOWNS.get(player.getUUID());
        if (playerCooldowns == null) return false;

        Long cooldownEnd = playerCooldowns.get(skillId);
        if (cooldownEnd == null) return false;

        return System.currentTimeMillis() < cooldownEnd;
    }

    // 获取技能剩余冷却时间（秒）
    public static int getSkillRemainingCooldown(Player player, ResourceLocation skillId) {
        Map<ResourceLocation, Long> playerCooldowns = PLAYER_SKILL_COOLDOWNS.get(player.getUUID());
        if (playerCooldowns == null) return 0;

        Long cooldownEnd = playerCooldowns.get(skillId);
        if (cooldownEnd == null) return 0;

        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? (int)((remaining + 999) / 1000) : 0;
    }

    // 开始技能冷却
    public static void startSkillCooldown(Player player, ResourceLocation skillId) {
        Long cooldownMs = SKILL_COOLDOWN_MAP.get(skillId);
        if (cooldownMs == null || cooldownMs <= 0) return;

        PLAYER_SKILL_COOLDOWNS
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(skillId, System.currentTimeMillis() + cooldownMs);
    }

    // 清除技能冷却
    public static void clearSkillCooldown(Player player, ResourceLocation skillId) {
        Map<ResourceLocation, Long> playerCooldowns = PLAYER_SKILL_COOLDOWNS.get(player.getUUID());
        if (playerCooldowns != null) {
            playerCooldowns.remove(skillId);
            if (playerCooldowns.isEmpty()) {
                PLAYER_SKILL_COOLDOWNS.remove(player.getUUID());
            }
        }
    }

    // 清除玩家所有技能冷却
    public static void clearAllSkillCooldowns(Player player) {
        PLAYER_SKILL_COOLDOWNS.remove(player.getUUID());
    }

    // 获取技能显示名称
    public static Component getDisplayName(ResourceLocation skillId) {
        return SKILL_DISPLAY_NAMES.getOrDefault(skillId,
                Component.literal(skillId.toString()));
    }

    public static void triggerCurrentSkill(Player player) {
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("尝试触发当前技能");
        }
        if (!HenshinSystem.INSTANCE.isTransformed(player)) return;

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return;

        FormConfig form = RiderRegistry.getForm(data.formId());
        if (form == null) return;

        ResourceLocation skillId = form.getCurrentSkillId(player);
        if (skillId != null) {
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

            // 只触发事件，不执行具体逻辑
            if (triggerSkillEvent(player, data.formId(), skillId)) {
                // 技能成功触发后开始冷却
                startSkillCooldown(player, skillId);
            }
        }
    }

    // 触发技能（只负责事件分发）
    public static boolean triggerSkillEvent(Player player, ResourceLocation formId, ResourceLocation skillId) {
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("触发技能事件");
        }

        // 触发Pre事件（可取消）
        SkillEvent.Pre preEvent = new SkillEvent.Pre(player, formId, skillId);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) return false;

        // 触发Post事件（实际执行逻辑的地方）
        NeoForge.EVENT_BUS.post(new SkillEvent.Post(player, formId, skillId));
        return true;
    }

    public static void rotateSkill(Player player) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)) return;

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("尝试轮转技能");
        }

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.debug("无data");
            return;
        }

        FormConfig form = RiderRegistry.getForm(data.formId());
        if (form == null) {
            RideBattleLib.LOGGER.debug("无形态");
            return;
        }

        RiderData riderData = player.getData(RiderAttachments.RIDER_DATA);
        int currentIndex = riderData.getCurrentSkillIndex();
        int skillCount = form.getSkillIds().size();

        if (skillCount > 0) {
            // 循环切换技能
            int newIndex = (currentIndex + 1) % skillCount;
            riderData.setCurrentSkillIndex(newIndex);

            // 获取新技能的ID
            ResourceLocation newSkill = form.getSkillIds().get(newIndex);
            Component displayName = SkillSystem.getDisplayName(newSkill);

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
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("显示轮转技能");
                }
                serverPlayer.displayClientMessage(displayName, true);
            }
        }
    }
    public static void clearPlayerCooldowns(UUID playerId) {
        PLAYER_SKILL_COOLDOWNS.remove(playerId);
    }
}