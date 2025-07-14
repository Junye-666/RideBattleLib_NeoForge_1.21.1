package com.jpigeon.ridebattlelib.core.system.skill;

import com.jpigeon.ridebattlelib.api.SkillHandler;
import com.jpigeon.ridebattlelib.core.system.event.SkillEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

public class SkillSystem {
    // 技能处理器注册表
    private static final Map<ResourceLocation, SkillHandler> SKILL_HANDLERS = new HashMap<>();

    // 技能显示名称注册表
    private static final Map<ResourceLocation, Component> SKILL_DISPLAY_NAMES = new HashMap<>();

    // 注册技能处理器和显示名称
    public static void registerSkill(ResourceLocation skillId, SkillHandler handler, Component displayName) {
        SKILL_HANDLERS.put(skillId, handler);
        SKILL_DISPLAY_NAMES.put(skillId, displayName);
    }

    // 获取技能显示名称
    public static Component getDisplayName(ResourceLocation skillId) {
        return SKILL_DISPLAY_NAMES.getOrDefault(skillId,
                Component.literal(skillId.toString())); // 默认返回ID字符串
    }

    // 触发技能
    public static boolean triggerSkill(Player player, ResourceLocation formId, ResourceLocation skillId) {
        // 触发Pre事件
        SkillEvent.Pre preEvent = new SkillEvent.Pre(player, formId, skillId);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) return false;

        // 执行技能
        SkillHandler handler = SKILL_HANDLERS.get(skillId);
        if (handler != null) {
            handler.execute(player, formId);
        }

        // 触发Post事件
        NeoForge.EVENT_BUS.post(new SkillEvent.Post(player, formId, skillId));
        return true;
    }
}