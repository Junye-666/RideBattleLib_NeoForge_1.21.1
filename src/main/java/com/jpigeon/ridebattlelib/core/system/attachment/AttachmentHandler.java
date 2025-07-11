package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinHelper;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Objects;

public class AttachmentHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        RideBattleLib.LOGGER.info("玩家登录: {} | 当前状态: {} | 变身数据: {}",
                player.getName().getString(),
                data.getHenshinState(),
                data.getTransformedData() != null ? "存在" : "不存在");

        if (data.getTransformedData() != null &&
                !player.getTags().contains("penalty_cooldown") &&
                !player.getTags().contains("just_respawned")) {

            // 确保状态正确设置为 TRANSFORMED
            HenshinSystem.INSTANCE.transitionToState(player, HenshinState.TRANSFORMED, null);

            // 恢复变身状态
            HenshinHelper.INSTANCE.restoreTransformedState(player, Objects.requireNonNull(data.getTransformedData()));

            RideBattleLib.LOGGER.info("已恢复玩家 {} 的变身状态", player.getName().getString());
        }

        if (data.isInPenaltyCooldown()) {
            player.addTag("penalty_cooldown");
        }

        if (player instanceof ServerPlayer serverPlayer) {
            // 确保腰带数据和变身状态都同步
            BeltSystem.INSTANCE.syncBeltData(serverPlayer);
            HenshinSystem.syncTransformedState(serverPlayer);
            HenshinSystem.syncHenshinState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        PlayerPersistentData originalData = original.getData(ModAttachments.PLAYER_DATA);

        // 只复制 riderBeltItems 和变身数据（但重生时不自动恢复）
        newPlayer.setData(ModAttachments.PLAYER_DATA, new PlayerPersistentData(
                new HashMap<>(originalData.riderBeltItems),
                new HashMap<>(originalData.auxBeltItems),
                originalData.getTransformedData(),
                originalData.getHenshinState(),
                originalData.getPendingFormId(),
                0
        ));

        PlayerPersistentData newData = newPlayer.getData(ModAttachments.PLAYER_DATA);
        newData.setHenshinState(HenshinState.IDLE);
        newData.setPendingFormId(null);

        // 添加重生标记
        newPlayer.addTag("just_respawned");
    }
}
