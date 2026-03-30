package com.jpigeon.ridebattlelib.server.handler;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.system.HenshinSystem;
import com.jpigeon.ridebattlelib.server.system.helper.SyncManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;

public class AttachmentHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("玩家登录: {} | 当前状态: {} | 变身数据: {}",
                    player.getName().getString(),
                    data.getState(),
                    data.getSessionData() != null ? "存在" : "不存在");
        }

        // 如果玩家处于惩罚冷却中，加上标签以便客户端知道（或用于其他逻辑）
        if (data.isInPenaltyCooldown()) {
            player.addTag("penalty_cooldown");
        } else {
            player.removeTag("penalty_cooldown");
        }

        // 如果玩家处于变身中（TRANSFORMING）状态，重置为 IDLE（登录时不可能在变身中）
        if (data.getState() == HenshinState.TRANSFORMING) {
            data.setState(HenshinState.IDLE);
            data.setPendingFormId(null);
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("重置玩家 {} 的状态为 IDLE，因为登录时处于 TRANSFORMING 状态", player.getName().getString());
            }
        }

        // 如果有变身会话数据且没有惩罚冷却标签和刚重生标签，则恢复变身
        if (data.getSessionData() != null &&
                !player.getTags().contains("penalty_cooldown") &&
                !player.getTags().contains("just_respawned")) {

            // 确保状态正确设置为 TRANSFORMED
            data.setState(HenshinState.TRANSFORMED);
            data.setPendingFormId(null); // 清除待处理形态

            // 恢复变身状态
            HenshinSessionData session = data.getSessionData();
            // 通过策略恢复状态
            // 注意：DefaultHenshinStrategy 需要从 session 中恢复装备、效果等
            HenshinUtils.restoreTransformedState(player, session);

            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("已恢复玩家 {} 的变身状态", player.getName().getString());
            }
        }

        // 同步到客户端
        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncAllPlayerData(serverPlayer);
        }
    }

    /**
     * 玩家死亡重生时：清除所有标记，并清除变身会话。
     * 注意：驱动器物品（持久数据）会保留，因为我们在克隆时复制了持久数据。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);

        // 清除所有标记
        player.removeTag("just_respawned");
        player.removeTag("penalty_cooldown");

        // 如果玩家处于变身状态，强制解除（但不清除驱动器物品）
        if (data.getSessionData() != null) {
            // 注意：这里不应该调用 unHenshin，因为 unHenshin 会调用策略并可能清空数据
            // 但为了安全，直接结束会话并清除效果即可（但最好通过策略清理）
            // 直接调用策略的 unHenshin 方法，但需要构造 TransformedData
            // 为了简化，我们直接清除会话数据，然后通过事件通知客户端
            data.endHenshinSession();
            data.setState(HenshinState.IDLE);
            data.setPendingFormId(null);
            // 同时需要从玩家身上移除盔甲和效果，但策略的 unHenshin 会做这些
            // 这里为了安全，直接调用 unHenshin 方法（但注意 unHenshin 会使用 sessionData）
            // 因为 sessionData 还存在，我们可以获取它
            HenshinSystem.getInstance().unHenshin(player);
        }

        // 确保同步
        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncAllPlayerData(serverPlayer);
        }
    }

    /**
     * 玩家克隆（死亡重生时） - 只复制持久数据，不清除临时数据。
     * 注意：原始玩家数据已在原实体上，新玩家是克隆体。
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        // 获取原始数据
        RiderData originalData = original.getData(RiderAttachments.RIDER_DATA);

        // 创建新数据，只复制持久数据（驱动器物品、冷却时间、技能索引）
        RiderData newData = new RiderData();
        // 复制主驱动器和辅助驱动器（深拷贝）
        newData.setMainDriverItems(deepCopyMap(originalData.getMainDriverItems()));
        newData.setAuxDriverItems(deepCopyMap(originalData.getAuxDriverItems()));
        newData.setPenaltyCooldownEnd(originalData.getPenaltyCooldownEnd());
        newData.setCurrentSkillIndex(originalData.getCurrentSkillIndex());

        // 注意：不复制会话数据（sessionData）和状态，重生后这些应重置
        // 状态默认 IDLE，会话数据 null

        // 设置新数据
        newPlayer.setData(RiderAttachments.RIDER_DATA, newData);

        // 添加重生标记，以便登录时判断
        newPlayer.addTag("just_respawned");

        // 如果有惩罚冷却，也加上标记
        if (newData.isInPenaltyCooldown()) {
            newPlayer.addTag("penalty_cooldown");
        }

        // 清理原始玩家的技能冷却（可选，避免残留）
        // SkillSystem.clearAllSkillCooldowns(newPlayer);
    }

    // 深拷贝工具方法（与 RiderData 内部一致，这里复用）
    private static Map<ResourceLocation, Map<ResourceLocation, ItemStack>> deepCopyMap(
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> original) {
        Map<ResourceLocation, Map<ResourceLocation, ItemStack>> copy = new HashMap<>();
        for (var entry : original.entrySet()) {
            Map<ResourceLocation, ItemStack> innerCopy = new HashMap<>();
            for (var innerEntry : entry.getValue().entrySet()) {
                innerCopy.put(innerEntry.getKey(), innerEntry.getValue().copy());
            }
            copy.put(entry.getKey(), innerCopy);
        }
        return copy;
    }
}
