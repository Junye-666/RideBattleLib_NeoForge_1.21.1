package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
public class HenshinSystem implements IHenshinSystem {
    private static final Map<UUID, TransformedData> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();
    public static final HenshinSystem INSTANCE = new HenshinSystem();

    public record TransformedData(
            RiderConfig config,
            ResourceLocation formId, // 新增形态ID
            Map<EquipmentSlot, ItemStack> originalGear
    ) {}

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        if (player.level().isClientSide()) {
            RideBattleLib.LOGGER.error("变身逻辑不能在客户端执行");
            return false;
        }

        if (isTransformed(player)) {
            RideBattleLib.LOGGER.warn("玩家已处于变身状态");
            return false;
        }

        RiderConfig config = RiderRegistry.getRider(riderId);

        if (config == null) {
            RideBattleLib.LOGGER.error("未找到骑士配置: {}", riderId);
            return false;
        }

        // 验证驱动器
        if (!validateDriver(player, config)) {
            RideBattleLib.LOGGER.warn("驱动器验证失败: 玩家未穿戴 {} 或槽位错误", config.getDriverItem());
            return false;
        }

        // 验证腰带物品
        if (!BeltSystem.INSTANCE.validateItems(player, riderId)) {
            RideBattleLib.LOGGER.warn("槽位验证失败: 必要槽位未正确填充");
            player.displayClientMessage(Component.translatable("ridebattlelib.validateItems.fail"), true);
            return false;
        }

        // 检查驱动器
        ItemStack driverStack = player.getItemBySlot(config.getDriverSlot());
        boolean isDriverValid = driverStack.is(config.getDriverItem());
        RideBattleLib.LOGGER.info("驱动器验证: 槽位 {} | 物品 {} | 有效: {}",
                config.getDriverSlot(), driverStack.getItem(), isDriverValid
        );
        if (!isDriverValid) {
            return false;
        }

        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);
        FormConfig form = config.getForm(formId);

        if (form == null) {
            RideBattleLib.LOGGER.error("未找到匹配的形态配置");
            return false;
        }
        HenshinEvent.Pre preEvent = new HenshinEvent.Pre(player, riderId, formId);

        // 执行变身逻辑
        RideBattleLib.LOGGER.info("玩家 {} 成功变身为 {}", player.getName(), riderId);
        Map<EquipmentSlot, ItemStack> originalGear = saveOriginalGear(player, config);
        beforeEquipArmor(player, () -> {
            // 使用form配置装备盔甲
            equipArmor(player, form);
            setTransformed(player, config, formId, originalGear);

            beforeApplyAttributes(player, () -> applyAttributes(player, form));
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
            // 移除属性
            removeAttributes(player, data.formId());

            restoreOriginalGear(player, data);
            syncEquipment(player);
            removeTransformed(player);
            BeltSystem.INSTANCE.returnItems(player);
        }
    }

    public boolean switchForm(Player player, ResourceLocation newFormId) {
        if (!isTransformed(player)) {
            return false;
        }

        TransformedData data = getTransformedData(player);
        if (data == null) {
            return false;
        }

        RiderConfig config = data.config();
        FormConfig newForm = config.getForm(newFormId);
        if (newForm == null) {
            return false;
        }

        // 移除旧形态效果
        removeAttributes(player, data.formId());

        // 应用新形态
        equipArmor(player, newForm);
        applyAttributes(player, newForm);

        // 更新变身数据
        setTransformed(player, config, newFormId, data.originalGear());

        // 触发事件
        NeoForge.EVENT_BUS.post(new FormSwitchEvent.Post(player, data.formId(), newFormId));
        return true;
    }

    //====================变身辅助方法====================

    public void equipArmor(Player player, FormConfig form) {
        if (form.getHelmet() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(form.getHelmet()));
        }
        if (form.getChestplate() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(form.getChestplate()));
        }
        if (form.getLeggings() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(form.getLeggings()));
        }
        if (form.getBoots() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(form.getBoots()));
        }
        syncEquipment(player);
    }

    private void restoreOriginalGear(Player player, TransformedData data) {
        if (data == null || player == null) return;

        // 恢复原装备（不包括驱动器）
        data.originalGear.forEach((slot, stack) -> {
            if (slot != data.config().getDriverSlot()) {
                player.setItemSlot(slot, stack);
            }
        });
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

    public Map<EquipmentSlot, ItemStack> saveOriginalGear(Player player, RiderConfig config) {
        Map<EquipmentSlot, ItemStack> originalGear = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == config.getDriverSlot()) {
                originalGear.put(slot, player.getItemBySlot(slot).copy());
            }
        }
        return originalGear;
    }

    private void applyAttributes(Player player, FormConfig form) {
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
        }
    }

    private void removeAttributes(Player player, ResourceLocation formId) {
        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) return;

        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        for (AttributeModifier modifier : form.getAttributes()) {
            Holder<Attribute> holder = attributeRegistry.getHolder(
                    ResourceKey.create(Registries.ATTRIBUTE, modifier.id())
            ).orElse(null);

            if (holder != null) {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifier.id()); // 使用UUID
                }
            }
        }

        for (MobEffectInstance effect : form.getEffects()) {
            player.removeEffect(effect.getEffect());
        }
    }


    //====================检查方法====================


    @Override
    public boolean isTransformed(Player player) {
        return player != null && TRANSFORMED_PLAYERS.containsKey(player.getUUID());
    }

    //====================Getter方法====================

    @Nullable
    public RiderConfig getConfig(Player player) {
        TransformedData data = getTransformedData(player);
        return data != null ? data.config() : null;
    }

    @Nullable
    public TransformedData getTransformedData(Player player) {
        return player != null ? TRANSFORMED_PLAYERS.get(player.getUUID()) : null;
    }

    //====================Setter方法====================

    public void setTransformed(Player player, RiderConfig config, ResourceLocation formId,
                               Map<EquipmentSlot, ItemStack> originalGear) {
        TRANSFORMED_PLAYERS.put(player.getUUID(),
                new TransformedData(config, formId, originalGear));
    }

    public void removeTransformed(Player player) {
        if (player != null) {
            TRANSFORMED_PLAYERS.remove(player.getUUID());
        }
    }
}
