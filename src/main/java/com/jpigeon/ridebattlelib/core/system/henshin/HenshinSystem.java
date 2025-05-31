package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IAnimationSystem;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltHandler;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.event.FormDynamicUpdateEvent;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事从此开始!
 * 假面骑士的变身系统
 */
public class HenshinSystem implements IHenshinSystem, IAnimationSystem {
    private static final Map<UUID, TransformedData> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();
    public static final HenshinSystem INSTANCE = new HenshinSystem();

    public record TransformedData(
            RiderConfig config,
            ResourceLocation formId,
            Map<EquipmentSlot, ItemStack> originalGear,
            Map<ResourceLocation, ItemStack> beltSnapshot // 新增字段
    ) {}

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        if (player.level().isClientSide()) {
            RideBattleLib.LOGGER.error("变身逻辑不能在客户端执行");
            return false;
        }

        if (isTransformed(player)) {
            // 已变身时改为形态切换而非拒绝
            RiderConfig currentConfig = getConfig(player);
            if (currentConfig != null && currentConfig.getRiderId().equals(riderId)) {
                Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
                ResourceLocation newFormId = currentConfig.matchForm(beltItems);
                switchForm(player, newFormId);
                return true;
            }
        }

        RiderConfig config = RiderRegistry.getRider(riderId);

        if (config == null) {
            RideBattleLib.LOGGER.error("未找到骑士配置: {}", riderId);
            return false;
        }

        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        if (beltItems == null) {
            RideBattleLib.LOGGER.error("腰带数据为空!");
            return false;
        }
        ResourceLocation formId = config.matchForm(beltItems);
        FormConfig form = config.getForm(formId);
        if (form == null) {
            RideBattleLib.LOGGER.error("未找到匹配的形态配置: {}", formId);
            return false;
        }

        HenshinEvent.Pre preEvent = new HenshinEvent.Pre(player, riderId, formId);
        // 播放开始动画
        playHenshinSequence(player, formId, AnimationPhase.START);


        RideBattleLib.LOGGER.info("玩家 {} 尝试变身为 {}", player.getName(), riderId);
        Map<EquipmentSlot, ItemStack> originalGear = saveOriginalGear(player, config);
        Map<ResourceLocation, ItemStack> beltSnapshot =
                new HashMap<>(BeltSystem.INSTANCE.getBeltItems(player));

        // 执行变身逻辑
        beforeEquipArmor(player, () -> {
            // 播放装备盔甲动画
            playHenshinSequence(player, formId, AnimationPhase.ARMOR_EQUIP);

            // 装备盔甲
            equipArmor(player, form, beltItems);
        });
        setTransformed(player, config, formId, originalGear, beltSnapshot);

        beforeApplyAttributes(player, () -> {
            applyAttributes(player, form, beltItems);

            // 播放结束动画
            playHenshinSequence(player, formId, AnimationPhase.END);

            // 触发变身完成事件
            NeoForge.EVENT_BUS.post(new HenshinEvent.Post(player, riderId, formId));
        });

        // 日志输出
        RideBattleLib.LOGGER.info("玩家 {} 变身为 {}，腰带数据: {}",
                player.getName(),
                riderId,
                BeltSystem.INSTANCE.getBeltItems(player)
        );

        return true;
    }

    @Override
    public void unHenshin(Player player) {
        TransformedData data = getTransformedData(player);
        if (data != null) {
            FormConfig form = RiderRegistry.getForm(data.formId());
            if (form != null) {
                Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
                List<MobEffectInstance> dynamicEffects = form.getDynamicEffects(beltItems);
                RideBattleLib.LOGGER.info("解除变身时将移除动态效果: {}", dynamicEffects);
            }

            // 移除属性
            removeAttributes(player, data.formId(), data.beltSnapshot);
            restoreOriginalGear(player, data);
            syncEquipment(player);
            removeTransformed(player);
            BeltSystem.INSTANCE.returnItems(player);
            onHenshinEnd(player);
        }
    }

    public void switchForm(Player player, ResourceLocation newFormId) {
        TransformedData data = getTransformedData(player);
        if (data == null) return;

        ResourceLocation currentFormId = data.formId();
        RideBattleLib.LOGGER.debug("收到切换形态请求: {} -> {}", currentFormId, newFormId);

        // 检查是否相同形态（需要处理动态部件更新）
        if (currentFormId.equals(newFormId)) {
            updateDynamicForm(player); // 调用动态更新方法
            return;
        }

        // 根据触发类型处理形态切换
        switch (data.config().getTriggerType()) {
            case KEY -> HenshinHandler.handleKeyFormSwitch(player, newFormId);
            case AUTO -> BeltHandler.handleAutoFormSwitch(player, newFormId);
            case ITEM -> TriggerItemHandler.handleItemFormSwitch(player, newFormId);
            default -> RideBattleLib.LOGGER.error("未知触发类型: {}", data.config().getTriggerType());
        }

        // 触发事件（仅在形态ID变化时）
        NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, currentFormId, newFormId));
    }

    //====================变身辅助方法====================

    public void equipArmor(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        // 先设置通用装备（固定槽位）
        RiderConfig config = getConfig(player);
        if (config != null) {
            for (Map.Entry<EquipmentSlot, Item> entry : config.getUniversalGear().entrySet()) {
                EquipmentSlot slot = entry.getKey();
                Item item = entry.getValue();
                if (item != Items.AIR) {
                    player.setItemSlot(slot, new ItemStack(item));
                }
            }
        }

        // 设置固定形态盔甲
        if (form.getHelmet() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(form.getHelmet()));
        }
        if (form.getChestplate() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(form.getChestplate()));
        }
        if (form.getLeggings() != null && form.getLeggings() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(form.getLeggings()));
        }
        if (form.getBoots() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(form.getBoots()));
        }

        // 设置动态部件盔甲（覆盖固定盔甲）
        for (ResourceLocation slotId : form.dynamicParts.keySet()) {
            // 安全获取物品堆栈，避免null
            ItemStack stack = beltItems.getOrDefault(slotId, ItemStack.EMPTY);
            if (stack.isEmpty()) {
                RideBattleLib.LOGGER.warn("动态部件槽位 {} 为空", slotId);
                continue;
            }

            FormConfig.DynamicPart part = form.dynamicParts.get(slotId);
            if (part == null) continue; // 额外的空检查

            Item armor = part.itemToArmor.get(stack.getItem());

            if (armor != null && armor != Items.AIR) {
                player.setItemSlot(part.slot, new ItemStack(armor));
            }
        }

        syncEquipment(player);
    }

    public void performFormSwitch(Player player, ResourceLocation newFormId) {
        TransformedData data = getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }

        ResourceLocation oldFormId = data.formId();
        FormConfig oldForm = RiderRegistry.getForm(oldFormId);
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        // 检查是否需要更新（形态ID变化或动态部件变化）
        boolean needsUpdate = !newFormId.equals(oldFormId);
        if (!needsUpdate && oldForm != null && newForm != null) {
            for (ResourceLocation slotId : newForm.dynamicParts.keySet()) {
                ItemStack newStack = currentBelt.get(slotId);
                ItemStack oldStack = data.beltSnapshot().get(slotId);
                if (!ItemStack.matches(newStack, oldStack)) {
                    needsUpdate = true;
                    break;
                }
            }
        }

        if (needsUpdate) {
            // 1. 移除旧效果
            removeAttributes(player, oldFormId, data.beltSnapshot());

            // 2. 应用新效果（如果新形态有效）
            if (newForm != null) {
                equipArmor(player, newForm, currentBelt);
                applyAttributes(player, newForm, currentBelt);
            }

            // 3. 更新变身状态
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt);

            // 4. 触发事件
            if (!newFormId.equals(oldFormId)) {
                NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, oldFormId, newFormId));
            } else {
                NeoForge.EVENT_BUS.post(new FormDynamicUpdateEvent(player, newFormId));
            }
        }
    }

    //====================辅助方法====================

    private void syncEquipment(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<Pair<EquipmentSlot, ItemStack>> slots = Arrays.stream(EquipmentSlot.values())
                    .map(slot -> Pair.of(slot, player.getItemBySlot(slot)))
                    .toList();
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(player.getId(), slots));
        }
    }

    public void restoreOriginalGear(Player player, TransformedData data) {
        if (data == null || player == null) return;

        // 恢复所有槽位，包括空槽位
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ||
                    slot == data.config().getDriverSlot()) {

                ItemStack original = data.originalGear().get(slot);

                // 如果原始装备为空，则清空槽位
                if (original == null || original.isEmpty()) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                } else {
                    player.setItemSlot(slot, original);
                }
            }
        }
        syncEquipment(player);
    }

    public Map<EquipmentSlot, ItemStack> saveOriginalGear(Player player, RiderConfig config) {
        Map<EquipmentSlot, ItemStack> originalGear = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ||
                    slot == config.getDriverSlot()) {

                ItemStack stack = player.getItemBySlot(slot);
                // 即使为空也要保存
                originalGear.put(slot, stack.copy());
            }
        }
        return originalGear;
    }

    public void applyAttributes(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        for (AttributeModifier modifier : form.getAttributes()) {
            // 通过注册表获取Holder
            attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).ifPresent(holder -> {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.addTransientModifier(modifier);
                }
            });
        }

        for (MobEffectInstance effect : form.getEffects()) {
            player.addEffect(new MobEffectInstance(effect));
            RideBattleLib.LOGGER.debug("应用效果: {} 给玩家 {}", effect, player.getName());
        }

        if (form.matchesDynamic(beltItems)) {
            for (MobEffectInstance effect : form.getDynamicEffects(beltItems)) {
                player.addEffect(new MobEffectInstance(effect));
            }
        }

    }

    public void removeAttributes(Player player, ResourceLocation formId, Map<ResourceLocation, ItemStack> beltSnapshot) {
        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;

        // 移除固定效果
        for (MobEffectInstance effect : form.getEffects()) {
            player.removeEffect(effect.getEffect());
        }

        // 移除动态效果（使用快照数据）
        for (ResourceLocation slotId : form.dynamicParts.keySet()) {
            ItemStack stack = beltSnapshot.getOrDefault(slotId, ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                FormConfig.DynamicPart part = form.dynamicParts.get(slotId);
                if (part != null) {
                    MobEffectInstance effect = part.itemToEffect.get(stack.getItem());
                    if (effect != null) {
                        player.removeEffect(effect.getEffect());
                    }
                }
            }
        }

        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        // 移除固定属性
        for (AttributeModifier modifier : form.getAttributes()) {
            Holder<Attribute> holder = attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).orElse(null);

            if (holder != null) {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifier.id());
                }
            }
        }

        // 移除固定效果
        for (MobEffectInstance effect : form.getEffects()) {
            player.removeEffect(effect.getEffect());
            RideBattleLib.LOGGER.debug("移除固定效果: {} 从玩家 {}", effect, player.getName());
        }

        // +++ 新增：移除动态效果 +++
        // 获取变身时的腰带数据
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        Map<ResourceLocation, ItemStack> beltItems = data.beltItems();

        // 移除动态效果
        if (!form.dynamicParts.isEmpty()) {
            for (ResourceLocation slotId : form.dynamicParts.keySet()) {
                ItemStack stack = beltItems.getOrDefault(slotId, ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    FormConfig.DynamicPart part = form.dynamicParts.get(slotId);
                    if (part != null) {
                        MobEffectInstance effect = part.itemToEffect.get(stack.getItem());
                        if (effect != null) {
                            player.removeEffect(effect.getEffect());
                            RideBattleLib.LOGGER.debug("移除动态效果: {} (来自槽位 {})", effect, slotId);
                        }
                    }
                }
            }
        }
    }

    //====================检查方法====================

    @Override
    public boolean isTransformed(Player player) {
        return player.getData(ModAttachments.PLAYER_DATA).transformedData() != null;
    }

    @Override
    public void playHenshinSequence(Player player, ResourceLocation formId, AnimationPhase phase) {
        // 触发动画事件供其他模组扩展
        NeoForge.EVENT_BUS.post(new AnimationEvent(player, formId, phase));
    }

    //====================Getter方法====================

    @Nullable
    public RiderConfig getConfig(Player player) {
        TransformedData data = getTransformedData(player);
        return data != null ? data.config() : null;
    }

    @Nullable
    public TransformedData getTransformedData(Player player) {
        TransformedAttachmentData attachmentData = player.getData(ModAttachments.PLAYER_DATA).transformedData();
        if (attachmentData == null) return null;

        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        if (config == null) return null;

        return new TransformedData(
                config,
                attachmentData.formId(),
                attachmentData.originalGear(),
                attachmentData.beltSnapshot() // 传递快照数据
        );
    }

    //====================Setter方法====================

    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId,
                               Map<EquipmentSlot, ItemStack> originalGear,
                               Map<ResourceLocation, ItemStack> beltSnapshot) {

        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);
        player.setData(ModAttachments.PLAYER_DATA,
                new PlayerPersistentData(oldData.beltItems(),
                        new TransformedAttachmentData(
                                config.getRiderId(),
                                formId,
                                originalGear,
                                beltSnapshot)));
    }

    public void removeTransformed(Player player) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);
        player.setData(ModAttachments.PLAYER_DATA,
                new PlayerPersistentData(oldData.beltItems(), null));
    }

    public void updateDynamicForm(Player player) {
        TransformedData data = getTransformedData(player);
        if (data == null) return;

        ResourceLocation formId = data.formId();
        FormConfig form = RiderRegistry.getForm(formId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        if (form != null && !form.dynamicParts.isEmpty()) {
            equipArmor(player, form, currentBelt);
            applyAttributes(player, form, currentBelt);
            setTransformed(player, data.config(), formId,
                    data.originalGear(), currentBelt);

            NeoForge.EVENT_BUS.post(new FormDynamicUpdateEvent(player, formId));
        }
    }
}
