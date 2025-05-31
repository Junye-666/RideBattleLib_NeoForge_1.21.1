package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
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
                                Objects.requireNonNull(data.transformedData()).originalGear(),
                                Objects.requireNonNull(data.transformedData()).beltSnapshot() // 新增
                        )
                );

                // 2. 重新装备盔甲（使用登录时的实时腰带数据）
                Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);
                HenshinSystem.INSTANCE.equipArmor(player, form, currentBelt);

                // 3. 重新应用属性（使用登录时的实时腰带数据）
                HenshinSystem.INSTANCE.applyAttributes(player, form, currentBelt);

                // 4. 更新变身状态（保留原始装备，更新腰带快照为当前状态）
                HenshinSystem.INSTANCE.setTransformed(player, config, formId,
                        Objects.requireNonNull(data.transformedData()).originalGear(),
                        currentBelt); // 更新为当前腰带状态

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
            RiderConfig config = RiderRegistry.getRider(Objects.requireNonNull(originalData.transformedData()).riderId());
            FormConfig form = RiderRegistry.getForm(Objects.requireNonNull(originalData.transformedData()).formId());

            if (config != null && form != null) {
                // 1. 恢复原始装备
                HenshinSystem.INSTANCE.restoreOriginalGear(newPlayer,
                        new HenshinSystem.TransformedData(
                                config,
                                Objects.requireNonNull(originalData.transformedData()).formId(),
                                Objects.requireNonNull(originalData.transformedData()).originalGear(),
                                Objects.requireNonNull(originalData.transformedData()).beltSnapshot() // 新增
                        )
                );

                // 2. 重新装备盔甲（使用复活时的实时腰带数据）
                Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(newPlayer);
                HenshinSystem.INSTANCE.equipArmor(newPlayer, form, currentBelt);

                // 3. 重新应用属性（使用复活时的实时腰带数据）
                HenshinSystem.INSTANCE.applyAttributes(newPlayer, form, currentBelt);

                // 4. 更新变身状态（保留原始装备，更新腰带快照为当前状态）
                HenshinSystem.INSTANCE.setTransformed(newPlayer, config,
                        Objects.requireNonNull(originalData.transformedData()).formId(),
                        Objects.requireNonNull(originalData.transformedData()).originalGear(),
                        currentBelt); // 更新为当前腰带状态
            }
        }
    }
}
