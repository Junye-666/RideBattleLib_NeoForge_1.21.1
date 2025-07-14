package com.jpigeon.ridebattlelib.core.system.skill;

import com.jpigeon.ridebattlelib.core.system.event.SkillEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    // 触发技能（只负责事件分发）
    public static boolean triggerSkill(Player player, ResourceLocation formId, ResourceLocation skillId) {
        // 触发Pre事件（可取消）
        SkillEvent.Pre preEvent = new SkillEvent.Pre(player, formId, skillId);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) return false;

        // 触发Post事件（实际执行逻辑的地方）
        NeoForge.EVENT_BUS.post(new SkillEvent.Post(player, formId, skillId));
        return true;
    }
}