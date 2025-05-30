package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Objects;

public class AttachmentHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        // 恢复腰带数据
        BeltSystem.INSTANCE.syncBeltData(player);

        // 恢复变身状态
        if (data.transformedData() != null) {
            ResourceLocation riderId = Objects.requireNonNull(data.transformedData()).riderId();
            ResourceLocation formId = Objects.requireNonNull(data.transformedData()).formId();
            RiderConfig config = RiderRegistry.getRider(riderId);
            FormConfig form = RiderRegistry.getForm(formId);

            if (config != null && form != null) {
                // 1. 恢复原始装备
                HenshinSystem.INSTANCE.restoreOriginalGear(player,
                        new HenshinSystem.TransformedData(
                                config,
                                formId,
                                Objects.requireNonNull(data.transformedData()).originalGear()
                        )
                );

                // 2. 重新装备盔甲
                HenshinSystem.INSTANCE.equipArmor(player, form);

                // 3. 重新应用属性
                HenshinSystem.INSTANCE.applyAttributes(player, form);

                // 4. 更新变身状态
                HenshinSystem.INSTANCE.setTransformed(player, config, formId,
                        Objects.requireNonNull(data.transformedData()).originalGear());

                RideBattleLib.LOGGER.info("恢复玩家变身状态: {}", player.getName());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        PlayerPersistentData originalData = original.getData(ModAttachments.PLAYER_DATA);

        // 复制数据到新玩家
        newPlayer.setData(ModAttachments.PLAYER_DATA, originalData);

        // 恢复变身状态
        if (originalData.transformedData() != null) {
            RiderConfig config = RiderRegistry.getRider(originalData.transformedData().riderId());
            FormConfig form = RiderRegistry.getForm(originalData.transformedData().formId());

            if (config != null && form != null) {
                // 1. 恢复原始装备
                HenshinSystem.INSTANCE.restoreOriginalGear(newPlayer,
                        new HenshinSystem.TransformedData(
                                config,
                                originalData.transformedData().formId(),
                                originalData.transformedData().originalGear()
                        )
                );

                // 2. 重新装备盔甲
                HenshinSystem.INSTANCE.equipArmor(newPlayer, form);

                // 3. 重新应用属性
                HenshinSystem.INSTANCE.applyAttributes(newPlayer, form);

                // 4. 更新变身状态
                HenshinSystem.INSTANCE.setTransformed(newPlayer, config,
                        Objects.requireNonNull(originalData.transformedData()).formId(),
                        Objects.requireNonNull(originalData.transformedData()).originalGear());
            }
        }
    }
}
