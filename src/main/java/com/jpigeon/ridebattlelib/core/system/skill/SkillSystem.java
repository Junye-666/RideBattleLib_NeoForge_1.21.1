package com.jpigeon.ridebattlelib.core.system.skill;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.event.SkillEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

public class SkillSystem {
    // 只保留技能名称注册
    private static final Map<ResourceLocation, Component> SKILL_DISPLAY_NAMES = new HashMap<>();

    // 注册技能显示名称
    public static void registerSkillName(ResourceLocation skillId, Component displayName) {
        SKILL_DISPLAY_NAMES.put(skillId, displayName);
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
            // 只触发事件，不执行具体逻辑
            triggerSkillEvent(player, data.formId(), skillId);
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

            if (player instanceof ServerPlayer serverPlayer) {
                // 显示切换提示
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("显示轮转技能");
                }
                serverPlayer.displayClientMessage(displayName, true);
            }
        }
    }
}
