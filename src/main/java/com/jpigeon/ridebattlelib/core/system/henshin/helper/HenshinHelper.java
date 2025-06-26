package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinHelper;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

/*
 * 变身辅助方法
 * performHenshin 执行变身过程
 * performFormSwitch 执行形态切换过程
 * restoreTransformedState (重连时)恢复变身后的状态
 * setTransformed 设置变身状态
 */

public final class HenshinHelper implements IHenshinHelper {
    public static final HenshinHelper INSTANCE = new HenshinHelper();
    public static final ArmorManager ARMOR = new ArmorManager();
    public static final EffectAndAttributeManager EFFECT_ATTRIBUTE = new EffectAndAttributeManager();
    public static final ItemManager ITEM = new ItemManager();

    @Override
    public void performHenshin(Player player, RiderConfig config, ResourceLocation formId) {
        if (config == null) return;
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        // 保存原始装备
        Map<EquipmentSlot, ItemStack> originalGear = ARMOR.saveOriginalGear(player, config);
        // 获取指定形态的配置
        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;
        // 根据玩家腰带上的物品匹配形态
        ResourceLocation matchedFormId = config.matchForm(beltItems);
        //给予物品
        ITEM.grantFormItems(player, matchedFormId);
        // 穿戴盔甲
        ARMOR.equipArmor(player, form, beltItems);
        // 应用属性和效果
        EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, form, beltItems);
        // 设置为已变身状态
        setTransformed(player, config, form.getFormId(), originalGear, beltItems);
        HenshinEvent.Post postHenshin = new HenshinEvent.Post(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(postHenshin);
    }

    @Override
    public void performFormSwitch(Player player, ResourceLocation newFormId) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }
        ResourceLocation oldFormId = data.formId();
        if (!newFormId.equals(oldFormId)) {
            FormSwitchEvent.Pre preFormSwitch = new FormSwitchEvent.Pre(player, oldFormId, newFormId);
            NeoForge.EVENT_BUS.post(preFormSwitch);
        }
        FormConfig oldForm = RiderRegistry.getForm(oldFormId);
        // 使用旧形态的腰带快照移除效果
        if (oldForm == null) return;
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);
        boolean needsUpdate = !newFormId.equals(oldFormId);
        if (newForm != null && needsUpdate) {
            // 装备新盔甲
            ARMOR.equipArmor(player, newForm, currentBelt);
            // 移除旧属性, 效果和物品
            EFFECT_ATTRIBUTE.removeAttributesAndEffects(player, oldFormId, data.beltSnapshot());
            ITEM.removeGrantedItems(player, oldFormId);
            // 应用新属性, 效果和物品
            EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, newForm, currentBelt);
            ITEM.grantFormItems(player, newFormId);
            // 更新数据
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt);
        }

        // 触发形态切换事件
        if (!newFormId.equals(oldFormId)) {
            FormSwitchEvent.Post postFormSwitch = new FormSwitchEvent.Post(player, oldFormId, newFormId);
            NeoForge.EVENT_BUS.post(postFormSwitch);
        }
    }

    @Override
    public void restoreTransformedState(Player player, TransformedAttachmentData attachmentData) {
        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        FormConfig form = RiderRegistry.getForm(attachmentData.formId());

        if (config != null && form != null) {
            // 恢复原始装备
            ARMOR.restoreOriginalGear(player, new HenshinSystem.TransformedData(
                    config,
                    attachmentData.formId(),
                    attachmentData.originalGear(),
                    attachmentData.beltSnapshot()
            ));

            // 重新装备盔甲
            ARMOR.equipArmor(player, form, attachmentData.beltSnapshot());

            // 重新应用属性
            EFFECT_ATTRIBUTE.applyAttributesAndEffects(player, form, attachmentData.beltSnapshot());

            // 更新变身状态
            setTransformed(player, config, attachmentData.formId(),
                    attachmentData.originalGear(), attachmentData.beltSnapshot());
        }
    }

    @Override
    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId, Map<EquipmentSlot, ItemStack> originalGear, Map<ResourceLocation, ItemStack> beltSnapshot) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);
        TransformedAttachmentData transformedData = new TransformedAttachmentData(
                config.getRiderId(),
                formId,
                originalGear,
                beltSnapshot
        );

        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems),
                transformedData,
                oldData.getHenshinState(),
                oldData.getPendingFormId(),
                oldData.getPenaltyCooldownEnd()
        );

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }

    @Override
    public void removeTransformed(Player player) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);

        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems),
                null,
                oldData.getHenshinState(),
                oldData.getPendingFormId(),
                oldData.getPenaltyCooldownEnd()
        );

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }
}