package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.attribute.AttributeCache;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.FormDynamicUpdateEvent;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HenshinCore {
    public static final Map<UUID, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();
    public static final HenshinCore INSTANCE = new HenshinCore();

    public static void executeTransform(Player player, RiderConfig config,
                                        ResourceLocation formId,
                                        Map<ResourceLocation, ItemStack> beltItems) {
        // 1. 检查变身冷却
        if (isOnCooldown(player)) {
            int remaining = (int) Math.ceil(
                    (INSTANCE.getHenshinCooldown() * 1000 -
                            (System.currentTimeMillis() - COOLDOWN_MAP.get(player.getUUID()))) / 1000.0
            );

            player.displayClientMessage(
                    Component.literal("变身冷却中! 剩余时间: " + remaining + "秒")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        // 检查吃瘪冷却
        if (PenaltySystem.PENALTY_SYSTEM.isInCooldown(player)) {

            return;
        }

        //保存原始装备
        Map<EquipmentSlot, ItemStack> originalGear = INSTANCE.saveOriginalGear(player, config);

        // 2. 装备盔甲
        FormConfig form = RiderRegistry.getForm(formId);
        if (form != null) {
            INSTANCE.equipArmor(player, form, beltItems);
        }

        // 3. 应用属性
        INSTANCE.applyAttributes(player, form, beltItems);

        // 4. 设置变身状态
        INSTANCE.setTransformed(player, config, formId,
                originalGear, new HashMap<>(beltItems));

        // 5. 触发事件
        startCooldown(player);
        NeoForge.EVENT_BUS.post(new HenshinEvent.Post(player, config.getRiderId(), formId));
    }

    public static void executeFormSwitch(Player player, ResourceLocation newFormId) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return;

        // 1. 清除旧效果
        INSTANCE.clearAllModEffects(player);

        // 2. 应用新形态
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        if (newForm != null) {
            // 3. 装备新盔甲
            INSTANCE.equipArmor(player, newForm, currentBelt);

            // 4. 应用新属性
            INSTANCE.applyAttributes(player, newForm, currentBelt);

            // 5. 更新数据
            INSTANCE.setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt);
        }

        // 6. 触发事件
        NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, data.formId(), newFormId));
    }

    public static void applyCachedAttributes(Player player, FormConfig form) {
        AttributeCache cache = AttributeCache.get(player);
        if (cache.requiresUpdate(form)) {
            INSTANCE.applyAttributes(player, form,
                    BeltSystem.INSTANCE.getBeltItems(player));
            cache.update(form);
        }
    }

    public int getHenshinCooldown() {
        return Config.HENSHIN_COOLDOWN.get();
    }

    public static boolean isOnCooldown(Player player) {
        Long lastHenshin = COOLDOWN_MAP.get(player.getUUID());
        int cooldown = INSTANCE.getHenshinCooldown() * 1000; // 转换为毫秒
        return lastHenshin != null &&
                (System.currentTimeMillis() - lastHenshin) < cooldown;
    }

    public static void startCooldown(Player player) {
        COOLDOWN_MAP.put(player.getUUID(), System.currentTimeMillis());
    }

    public int getRemainingCooldown(Player player) {
        Long lastHenshin = HenshinCore.COOLDOWN_MAP.get(player.getUUID());
        if (lastHenshin == null) return 0;

        int cooldown = Config.HENSHIN_COOLDOWN.get() * 1000;
        long elapsed = System.currentTimeMillis() - lastHenshin;
        long remaining = Math.max(0, cooldown - elapsed);
        return (int) Math.ceil(remaining / 1000.0);
    }

    public void equipArmor(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        // 先设置通用装备（固定槽位）
        RiderConfig config = HenshinSystem.INSTANCE.getConfig(player);
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

    public void clearAllModEffects(Player player) {
        // 清除所有固定效果
        for (FormConfig form : RiderRegistry.getAllForms()) {
            for (MobEffectInstance effect : form.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }

        // 清除所有可能的动态效果
        for (FormConfig form : RiderRegistry.getAllForms()) {
            for (FormConfig.DynamicPart part : form.dynamicParts.values()) {
                for (MobEffectInstance effect : part.itemToEffect.values()) {
                    player.removeEffect(effect.getEffect());
                }
            }
        }

        RideBattleLib.LOGGER.warn("强制清除玩家所有模组效果: {}", player.getName());
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

    public void restoreOriginalGear(Player player, HenshinSystem.TransformedData data) {
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

        // === 修复：只在驱动器槽位丢失时才补充驱动器 ===
        EquipmentSlot driverSlot = data.config().getDriverSlot();
        ItemStack currentDriver = player.getItemBySlot(driverSlot);

        // 检查当前驱动器槽位是否是正确的驱动器
        boolean hasDriverInSlot = !currentDriver.isEmpty() &&
                currentDriver.is(data.config().getDriverItem());

        // 如果驱动器槽位没有正确驱动器，才检查背包
        if (!hasDriverInSlot) {
            boolean hasDriverInInventory = false;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.is(data.config().getDriverItem())) {
                    hasDriverInInventory = true;
                    break;
                }
            }

            // 如果整个背包都没有驱动器，才返还一个
            if (!hasDriverInInventory) {
                ItemStack driver = new ItemStack(data.config().getDriverItem());
                if (!player.addItem(driver)) {
                    player.drop(driver, false);
                }
            }
        }
        syncEquipment(player);
    }

    public void syncEquipment(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<Pair<EquipmentSlot, ItemStack>> slots = Arrays.stream(EquipmentSlot.values())
                    .map(slot -> Pair.of(slot, player.getItemBySlot(slot)))
                    .toList();
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(player.getId(), slots));
        }
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

    public void removeAttributes(Player player, ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems) {
        // 先强制清除所有效果（三重保障）
        HenshinCore.INSTANCE.clearAllModEffects(player);

        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;

        // 移除属性修饰符
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;
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

        // 记录并报告任何残留效果
        for (Holder<MobEffect> activeEffect : player.getActiveEffectsMap().keySet()) {
            RideBattleLib.LOGGER.warn("残留效果: {}", BuiltInRegistries.MOB_EFFECT.getKey((MobEffect) activeEffect));
        }
    }

    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId,
                               Map<EquipmentSlot, ItemStack> originalGear,
                               Map<ResourceLocation, ItemStack> beltSnapshot) {

        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);

        // 创建新的变身数据
        TransformedAttachmentData transformedData = new TransformedAttachmentData(
                config.getRiderId(),
                formId,
                originalGear,
                beltSnapshot
        );

        // 创建新的持久化数据（使用新的 riderBeltItems 结构）
        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems), // 复制原有的 riderBeltItems
                transformedData
        );

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }

    public void removeTransformed(Player player) {
        PlayerPersistentData oldData = player.getData(ModAttachments.PLAYER_DATA);

        // 创建新的持久化数据（保留 riderBeltItems，清除变身数据）
        PlayerPersistentData newData = new PlayerPersistentData(
                new HashMap<>(oldData.riderBeltItems), // 复制原有的 riderBeltItems
                null // 清除变身数据
        );

        player.setData(ModAttachments.PLAYER_DATA, newData);
    }

    public void performFormSwitch(Player player, ResourceLocation newFormId) {
        // 在切换形态前强制清除所有效果
        HenshinCore.INSTANCE.clearAllModEffects(player);

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }

        ResourceLocation oldFormId = data.formId();

        // 即使形态ID相同也强制移除旧效果
        removeAttributes(player, oldFormId, data.beltSnapshot());

        FormConfig oldForm = RiderRegistry.getForm(oldFormId);

        // === 先移除旧形态的所有效果和属性 ===
        if (oldForm != null) {
            // 使用旧形态的腰带快照移除效果
            removeAttributes(player, oldFormId, data.beltSnapshot());
        }
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        // 获取当前实时腰带数据
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        // 检查是否需要更新
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
            // 使用当前腰带数据移除旧效果（关键修改）
            removeAttributes(player, oldFormId, currentBelt);

            // 应用新效果
            if (newForm != null) {
                equipArmor(player, newForm, currentBelt);
                applyAttributes(player, newForm, currentBelt);
            }

            // 更新变身数据（同步当前腰带快照）
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt); // 更新为当前腰带状态

            // 触发事件
            if (!newFormId.equals(oldFormId)) {
                NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, oldFormId, newFormId));
            } else {
                NeoForge.EVENT_BUS.post(new FormDynamicUpdateEvent(player, newFormId));
            }
        }
    }

    public static void updateCooldownEffects(Player player) {
        if (isOnCooldown(player)) {
            int remaining = INSTANCE.getRemainingCooldown(player);

            // 添加发光效果
            player.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    40,  // 持续2秒
                    0,
                    false,
                    false
            ));

            // 每5秒显示一次提示
            if (player.tickCount % 100 == 0) {
                player.displayClientMessage(
                        Component.literal("变身冷却中! 剩余: " + remaining + "秒")
                                .withStyle(ChatFormatting.YELLOW),
                        true
                );
            }
        }
    }
}
