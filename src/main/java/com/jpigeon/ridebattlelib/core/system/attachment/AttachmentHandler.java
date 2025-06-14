package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinHelper;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
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

        if (player instanceof ServerPlayer serverPlayer) {
            // 确保腰带数据和变身状态都同步
            BeltSystem.INSTANCE.syncBeltData(serverPlayer);
            HenshinSystem.syncTransformedState(serverPlayer);
        }
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        // 只有在玩家不是死亡重生且没有吃瘪冷却时才恢复变身状态
        if (data.transformedData() != null &&
                !player.getTags().contains("penalty_cooldown") &&
                !player.getTags().contains("just_respawned")) {
            HenshinHelper.INSTANCE.restoreTransformedState(player, Objects.requireNonNull(data.transformedData()));
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
                new HashMap<>(originalData.riderBeltItems), // 复制 riderBeltItems
                originalData.transformedData() // 保留变身数据但不自动恢复
        ));

        // 添加重生标记
        newPlayer.addTag("just_respawned");
    }
}
