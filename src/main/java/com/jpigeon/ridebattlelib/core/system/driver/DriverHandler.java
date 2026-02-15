package com.jpigeon.ridebattlelib.core.system.driver;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.RiderManager;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.SyncManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import com.jpigeon.ridebattlelib.core.system.network.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.DriverActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RideBattleLib.MODID, value = Dist.DEDICATED_SERVER)
public class DriverHandler {
    private static final Map<UUID, Long> LAST_INTERACTION_TIME = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) return;
        Player player = event.getEntity();
        if (isInteractionOnCooldown(player)) {
            event.setCanceled(true);
            return;
        }

        if (RiderRegistry.getRegisteredRiders().isEmpty()) return;
        if (event.getSide() != LogicalSide.SERVER) return;

        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) return;

        if (player.getCooldowns().isOnCooldown(heldItem.getItem())) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("物品处于冷却中，取消操作: {}", heldItem.getItem());
            }
            event.setCanceled(true);
            return;
        }

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        setInteractionCooldown(player);

        if (heldItem.is(config.getTriggerItem())) {
            handleTriggerItem(event, player, heldItem, config);
        } else {
            handleItemInsertion(event, player, heldItem, config);
        }
    }

    // 检查玩家操作冷却
    private static boolean isInteractionOnCooldown(Player player) {
        Long lastInteraction = LAST_INTERACTION_TIME.get(player.getUUID());
        if (lastInteraction == null) return false;

        return System.currentTimeMillis() - lastInteraction < Config.INTERACTION_COOLDOWN_MS.get();
    }

    // 设置操作冷却
    private static void setInteractionCooldown(Player player) {
        LAST_INTERACTION_TIME.put(player.getUUID(), System.currentTimeMillis());
    }

    // 处理触发物品逻辑
    private static void handleTriggerItem(PlayerInteractEvent.RightClickItem event, Player player,
                                          ItemStack heldItem, RiderConfig config) {
        // 取消事件传播，避免物品被消耗
        event.setCanceled(true);

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        ResourceLocation formId = data.getPendingFormId();
        FormConfig formConfig = RiderRegistry.getForm(formId);

        // 触发变身逻辑
        if (formConfig != null && formConfig.getTriggerType() == TriggerType.ITEM) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("检测到ITEM驱动方式");
                RideBattleLib.LOGGER.debug("物品触发 - 玩家状态: 变身={}, 驱动器={}",
                        HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
            }
            PacketHandler.sendToServer(new DriverActionPacket(player.getUUID()));
        }

        // 强制恢复物品数量（防止NBT修改）
        if (!player.isCreative()) {
            heldItem.setCount(heldItem.getCount() + 1);
        }
    }

    // 处理物品插入逻辑（原DriverHandler的功能）
    private static void handleItemInsertion(PlayerInteractEvent.RightClickItem event, Player player,
                                            ItemStack heldItem, RiderConfig config) {
        boolean isTransformed = HenshinSystem.INSTANCE.isTransformed(player);

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("右键插入物品 - 玩家: {}, 变身状态: {}, 骑士: {}",
                    player.getName().getString(), isTransformed, config.getRiderId());
        }

        boolean inserted = false;

        // 创建要插入的物品副本（只复制1个）
        ItemStack itemToInsert = heldItem.copyWithCount(1);

        // 先尝试主驱动器槽位
        for (ResourceLocation slotId : config.getSlotDefinitions().keySet()) {
            if (DriverSystem.INSTANCE.insertItem(player, slotId, itemToInsert)) {
                heldItem.shrink(1);
                inserted = true;
                break;
            }
        }

        // 再尝试辅助驱动器槽位
        if (!inserted && config.hasAuxDriverEquipped(player)) {
            for (ResourceLocation slotId : config.getAuxSlotDefinitions().keySet()) {
                if (DriverSystem.INSTANCE.insertItem(player, slotId, itemToInsert)) {
                    heldItem.shrink(1);
                    inserted = true;
                    break;
                }
            }
        }

        if (inserted) {
            if (player instanceof ServerPlayer serverPlayer) {
                SyncManager.INSTANCE.syncDriverData(serverPlayer);
            }

            FormConfig formConfig = config.getActiveFormConfig(player);
            if (formConfig != null && Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("形态触发类型: {}", formConfig.getTriggerType());
            }

            // 添加 null 检查
            if (formConfig != null && formConfig.getTriggerType() == TriggerType.AUTO) {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("自动触发 - 玩家状态: 变身={}, 驱动器={}",
                            HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
                }
                PacketHandler.sendToServer(new DriverActionPacket(player.getUUID()));
            }

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // 是否处于变身状态
        if (!RiderManager.isTransformed(player)) return;
        RiderConfig config = RiderManager.getActiveRiderConfig(player);
        if (config == null) return;

        // 只关心骑士驱动器槽位
        EquipmentSlot slot = event.getSlot();
        if (!isRiderDriverSlot(config, slot)) return;

        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        if (isRiderDriver(to)) return;
        // 如果试图移除驱动器
        if (isRiderDriver(from)) {
            RiderManager.unTransform(player);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        ItemStack stack = event.getItemEntity().getItem();
        if (isRiderArmor(stack)) {
            stack.shrink(stack.getCount());
        }
    }


    private static boolean isRiderDriverSlot(RiderConfig config, EquipmentSlot slot) {
        return slot.equals(config.getDriverSlot());
    }

    private static boolean isRiderArmor(ItemStack stack) {
        return RiderArmorRegistry.getAllArmor().contains(stack.getItem());
    }

    private static boolean isRiderDriver(ItemStack stack) {
        return RiderArmorRegistry.getAllDriver().contains(stack.getItem());
    }

}
