package com.jpigeon.ridebattlelib.core.system.henshin.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.KeyBindings;

import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.ReturnItemsPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.UnhenshinPacket;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;


public class KeyHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (KeyBindings.DRIVER_KEY.consumeClick()) {
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config == null) return;

            FormConfig formConfig = config.getActiveFormConfig(player);
            if (formConfig != null && formConfig.getTriggerType() == TriggerType.KEY) {
                RideBattleLib.LOGGER.info("按键触发 - 玩家状态: 变身={}, 驱动器={}", HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
                HenshinSystem.INSTANCE.driverAction(player);
            }
        }
        if (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            RideBattleLib.LOGGER.debug("发送解除变身数据包");
            PacketHandler.sendToServer(new UnhenshinPacket(player.getUUID()));
        }

        if (KeyBindings.RETURN_ITEMS_KEY.consumeClick()) {
            // 触发物品返还
            PacketHandler.sendToServer(new ReturnItemsPacket());
        }

        if (KeyBindings.SKILL_KEY.consumeClick()) {
            // 蹲下时切换技能，否则触发当前技能
            if (player.isShiftKeyDown()) {
                rotateSkill(player);
            } else {
                triggerCurrentSkill(player);
            }
        }
    }


    private static void triggerCurrentSkill(Player player) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)) return;

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return;

        FormConfig form = RiderRegistry.getForm(data.formId());
        if (form == null) return;

        ResourceLocation skillId = form.getCurrentSkillId(player);
        if (skillId != null) {
            // 只触发事件，不执行具体逻辑
            SkillSystem.triggerSkill(player, data.formId(), skillId);
        }
    }

    // 切换技能轮盘
    private static void rotateSkill(Player player) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)) return;

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return;

        FormConfig form = RiderRegistry.getForm(data.formId());
        if (form == null) return;

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

            // 显示切换提示
            player.displayClientMessage(
                    displayName,
                    true
            );
        }
    }
}
