package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.KeyBindings;
import com.jpigeon.ridebattlelib.core.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.network.packet.ReturnItemsPacket;
import com.jpigeon.ridebattlelib.core.network.packet.SwitchFormPacket;
import com.jpigeon.ridebattlelib.core.network.packet.UnhenshinPacket;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.Map;
import java.util.Objects;

public class HenshinHandler {
    // 右键变身逻辑已迁移至BeltHandler中
    // 按键变身逻辑
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (KeyBindings.HENSHIN_KEY.consumeClick()) {
            RiderConfig activeConfig = RiderConfig.findActiveDriverConfig(player);
            if (activeConfig != null) {
                if (HenshinSystem.INSTANCE.isTransformed(player)) {
                    // 形态切换逻辑
                    Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
                    ResourceLocation newFormId = activeConfig.matchForm(beltItems);

                    if (!newFormId.equals(Objects.requireNonNull(HenshinSystem.INSTANCE.getTransformedData(player)).formId())) {
                        PacketHandler.sendToServer(new SwitchFormPacket(newFormId));
                    }
                } else if (activeConfig.getTriggerType() == TriggerType.KEY) {
                    // 标准变身逻辑
                    PacketHandler.sendToServer(new HenshinPacket(activeConfig.getRiderId()));
                }
            }
        }
        if (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            RideBattleLib.LOGGER.debug("发送解除变身数据包");
            PacketHandler.sendToServer(new UnhenshinPacket());
        }
        if (KeyBindings.RETURN_ITEMS_KEY.consumeClick()) {
            // 触发物品返还
            PacketHandler.sendToServer(new ReturnItemsPacket());
        }
    }
}
