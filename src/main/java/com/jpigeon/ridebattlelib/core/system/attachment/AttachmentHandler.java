package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinHelper;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import io.netty.handler.logging.LogLevel;
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
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
            RideBattleLib.LOGGER.debug("玩家登录: {} | 当前状态: {} | 变身数据: {}",
                    player.getName().getString(),
                    data.getHenshinState(),
                    data.getTransformedData() != null ? "存在" : "不存在");
        }

        if (data.getTransformedData() != null &&
                !player.getTags().contains("penalty_cooldown") &&
                !player.getTags().contains("just_respawned")) {

            // 确保状态正确设置为 TRANSFORMED
            HenshinSystem.INSTANCE.transitionToState(player, HenshinState.TRANSFORMED, null);

            // 恢复变身状态
            HenshinHelper.INSTANCE.restoreTransformedState(player, Objects.requireNonNull(data.getTransformedData()));
            if (Config.LOG_LEVEL.get().equals(LogLevel.DEBUG)) {
                RideBattleLib.LOGGER.debug("已恢复玩家 {} 的变身状态", player.getName().getString());
            }
        }

        if (data.isInPenaltyCooldown()) {
            player.addTag("penalty_cooldown");
        }

        if (data.getHenshinState() == HenshinState.TRANSFORMING) {
            data.setHenshinState(HenshinState.IDLE);
            data.setPendingFormId(null);
            RideBattleLib.LOGGER.debug("重置玩家 {} 的状态为IDLE，因为登录时处于TRANSFORMING状态",
                    player.getName().getString());
        }


        if (player instanceof ServerPlayer serverPlayer) {
            // 确保驱动器数据和变身状态都同步
            DriverSystem.INSTANCE.syncDriverData(serverPlayer);
            HenshinSystem.syncHenshinState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        RiderData originalData = original.getData(RiderAttachments.RIDER_DATA);

        // 只复制 mainDriverItems 和变身数据（但重生时不自动恢复）
        newPlayer.setData(RiderAttachments.RIDER_DATA, new RiderData(
                new HashMap<>(originalData.mainDriverItems),
                new HashMap<>(originalData.auxDriverItems),
                originalData.getTransformedData(),
                originalData.getHenshinState(),
                originalData.getPendingFormId(),
                0,
                originalData.getCurrentSkillIndex()
        ));

        RiderData newData = newPlayer.getData(RiderAttachments.RIDER_DATA);
        newData.setHenshinState(HenshinState.IDLE);
        newData.setPendingFormId(null);

        // 添加重生标记
        newPlayer.addTag("just_respawned");
    }
}
