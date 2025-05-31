package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Objects;


public class AttachmentHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // 登录时强制完整同步
        if (player instanceof ServerPlayer serverPlayer) {
            BeltSystem.INSTANCE.syncBeltData(serverPlayer);
        }
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        if (data.transformedData() != null) {
            HenshinSystem.INSTANCE.restoreTransformedState(player, Objects.requireNonNull(data.transformedData()));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        PlayerPersistentData originalData = original.getData(ModAttachments.PLAYER_DATA);

        newPlayer.setData(ModAttachments.PLAYER_DATA, new PlayerPersistentData(
                originalData.beltItems(),
                originalData.transformedData()
        ));

        if (originalData.transformedData() != null) {
            HenshinSystem.INSTANCE.restoreTransformedState(newPlayer, Objects.requireNonNull(originalData.transformedData()));
        }
    }
}