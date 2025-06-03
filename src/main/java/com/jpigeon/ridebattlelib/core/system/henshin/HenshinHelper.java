package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinHelper;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HenshinHelper implements IHenshinHelper {
    public static final HenshinHelper INSTANCE = new HenshinHelper();
    public static final Map<UUID, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();

    @Override
    public void executeTransform(Player player, RiderConfig config, ResourceLocation formId) {
        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        Map<EquipmentSlot, ItemStack> originalGear = saveOriginalGear(player, config);

        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;

        equipArmor(player, form, beltItems);
        applyAttributesAndEffects(player, form, beltItems);
        setTransformed(player, config, formId, originalGear, beltItems);

        NeoForge.EVENT_BUS.post(new HenshinEvent.Post(player, config.getRiderId(), formId));
    }

    @Override
    public void executeFormSwitch(Player player, ResourceLocation newFormId) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return;

        ResourceLocation oldFormId = data.formId();

        // 1. 移除旧形态的属性和效果
        removeAttributesAndEffects(player, oldFormId, data.beltSnapshot());

        // 2. 应用新形态
        FormConfig newForm = RiderRegistry.getForm(newFormId);
        Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

        if (newForm != null) {
            // 3. 装备新盔甲
            INSTANCE.equipArmor(player, newForm, currentBelt);

            // 4. 应用新属性和效果
            INSTANCE.applyAttributesAndEffects(player, newForm, currentBelt);

            // 5. 更新数据
            INSTANCE.setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt);
        }

        // 6. 触发事件
        NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, data.formId(), newFormId));
    }

    @Override
    public void restoreTransformedState(Player player, TransformedAttachmentData attachmentData) {
        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        FormConfig form = RiderRegistry.getForm(attachmentData.formId());

        if (config != null && form != null) {
            // 恢复原始装备
            HenshinHelper.INSTANCE.restoreOriginalGear(player, new HenshinSystem.TransformedData(
                    config,
                    attachmentData.formId(),
                    attachmentData.originalGear(),
                    attachmentData.beltSnapshot()
            ));

            // 重新装备盔甲
            HenshinHelper.INSTANCE.equipArmor(player, form, attachmentData.beltSnapshot());

            // 重新应用属性
            HenshinHelper.INSTANCE.applyAttributesAndEffects(player, form, attachmentData.beltSnapshot());

            // 更新变身状态
            HenshinHelper.INSTANCE.setTransformed(player, config, attachmentData.formId(),
                    attachmentData.originalGear(), attachmentData.beltSnapshot());
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
        Long lastHenshin = HenshinHelper.COOLDOWN_MAP.get(player.getUUID());
        if (lastHenshin == null) return 0;

        int cooldown = Config.HENSHIN_COOLDOWN.get() * 1000;
        long elapsed = System.currentTimeMillis() - lastHenshin;
        long remaining = Math.max(0, cooldown - elapsed);
        return (int) Math.ceil(remaining / 1000.0);
    }

    public void equipArmor(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        // 先设置通用装备（固定槽位）
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

        // 确保盔甲立即生效
        syncEquipment(player);
    }

    public void clearAllModEffects(Player player) {
        // 清除所有固定效果
        for (FormConfig form : RiderRegistry.getAllForms()) {
            for (MobEffectInstance effect : form.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }

        RideBattleLib.LOGGER.warn("强制清除玩家所有模组效果: {}", player.getName());
    }

    public void removeAttributesAndEffects(Player player, ResourceLocation formId,
                                           Map<ResourceLocation, ItemStack> beltItems) {
        removeAttributes(player, formId, beltItems);
        removeEffects(player, formId);
    }

    // 添加效果移除方法
    public void removeEffects(Player player, ResourceLocation formId) {
        FormConfig form = RiderRegistry.getForm(formId);
        if (form != null) {
            for (MobEffectInstance effect : form.getEffects()) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    // 添加新方法：应用属性和效果
    public void applyAttributesAndEffects(Player player, FormConfig form,
                                          Map<ResourceLocation, ItemStack> beltItems) {
        applyAttributes(player, form, beltItems);
        applyEffects(player, form);
    }

    // 添加效果应用方法
    public void applyEffects(Player player, FormConfig form) {
        for (MobEffectInstance effect : form.getEffects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
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
                    .map(slot -> {
                        ItemStack stack = player.getItemBySlot(slot);
                        // 确保盔甲耐久度正确显示
                        if (stack.isDamageableItem()) {
                            stack.setDamageValue(0);
                        }
                        return Pair.of(slot, stack);
                    })
                    .toList();

            // 强制同步所有装备槽位
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(player.getId(), slots));
        }
    }

    public void applyAttributes(Player player, FormConfig form, Map<ResourceLocation, ItemStack> beltItems) {
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        // 先移除可能存在的旧属性
        for (AttributeModifier modifier : form.getAttributes()) {
            attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).ifPresent(holder -> {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifier.id()); // 先移除
                }
            });
        }

        // 再应用新属性
        for (AttributeModifier modifier : form.getAttributes()) {
            attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).ifPresent(holder -> {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.addTransientModifier(modifier); // 后添加
                }
            });
        }
    }

    public void removeAttributes(Player player, ResourceLocation formId, Map<ResourceLocation, ItemStack> beltItems) {
        // 先强制清除所有效果
        HenshinHelper.INSTANCE.clearAllModEffects(player);

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
            activeEffect.unwrapKey().ifPresent(key -> RideBattleLib.LOGGER.warn("残留效果: {}", key.location()));
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
        HenshinHelper.INSTANCE.clearAllModEffects(player);

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

        if (needsUpdate) {
            // 使用当前腰带数据移除旧效果（关键修改）
            removeAttributes(player, oldFormId, currentBelt);

            // 应用新效果
            if (newForm != null) {
                equipArmor(player, newForm, currentBelt);
                applyAttributesAndEffects(player, newForm, currentBelt);
            }

            // 更新变身数据（同步当前腰带快照）
            setTransformed(player, data.config(), newFormId,
                    data.originalGear(), currentBelt); // 更新为当前腰带状态

            // 触发事件
            if (!newFormId.equals(oldFormId)) {
                NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, oldFormId, newFormId));
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
