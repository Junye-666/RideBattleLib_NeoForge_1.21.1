package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Objects;

public class AttachmentHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        // 初始化腰带数据（如果为空）
        if (data.beltItems() == null) {
            player.setData(ModAttachments.PLAYER_DATA,
                    new PlayerPersistentData(new HashMap<>(), data.transformedData()));
        }

        // 恢复变身状态
        if (data.transformedData() != null) {
            ResourceLocation riderId = Objects.requireNonNull(data.transformedData()).riderId();
            ResourceLocation formId = Objects.requireNonNull(data.transformedData()).formId();
            RiderConfig config = RiderRegistry.getRider(riderId);
            FormConfig form = RiderRegistry.getForm(formId);

            if (config != null && form != null) {
                // 重新装备盔甲
                HenshinSystem.INSTANCE.equipArmor(player, form);

                // 重新应用属性
                HenshinSystem.INSTANCE.applyAttributes(player, form);

                RideBattleLib.LOGGER.info("恢复玩家变身状态: {}", player.getName());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        // 复制持久化数据
        PlayerPersistentData originalData = original.getData(ModAttachments.PLAYER_DATA);
        newPlayer.setData(ModAttachments.PLAYER_DATA, originalData);

        // 恢复变身状态
        if (originalData.transformedData() != null) {
            RiderConfig config = RiderRegistry.getRider(Objects.requireNonNull(originalData.transformedData()).riderId());
            FormConfig form = RiderRegistry.getForm(Objects.requireNonNull(originalData.transformedData()).formId());

            if (config != null && form != null) {
                HenshinSystem.INSTANCE.equipArmor(newPlayer, form);
                HenshinSystem.INSTANCE.applyAttributes(newPlayer, form);
            }
        }
    }
}
