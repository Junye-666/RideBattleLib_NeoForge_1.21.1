package com.jpigeon.ridebattlelib.client.network;

import com.jpigeon.ridebattlelib.client.cache.ClientDriverDataCache;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.common.api.client.ClientRiderContext;
import com.jpigeon.ridebattlelib.common.api.client.ClientRiderDispatcher;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataDiffPacket;
import com.jpigeon.ridebattlelib.common.network.packet.DriverDataSyncPacket;
import com.jpigeon.ridebattlelib.common.network.packet.HenshinStateSyncPacket;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ClientRiderSyncManager {
    private ClientRiderSyncManager() {
    }

    // ========== 变身状态同步 ==========
    public static void applyHenshinState(HenshinStateSyncPacket p) {
        Minecraft.getInstance().execute(() -> {
            UUID playerId = p.playerId();

            // 读取旧状态（必须在 update 之前）
            boolean wasTransformed = ClientTransformedCache.isTransformed(playerId);
            ResourceLocation oldFormId = ClientTransformedCache.getCurrentFormId(playerId);
            ResourceLocation oldRiderId = ClientTransformedCache.getRiderId(playerId);

            // 写新状态
            ClientTransformedCache.update(
                    playerId,
                    p.isTransformed(),
                    p.state(),
                    p.riderId(),
                    p.currentFormId(),
                    p.pendingFormId()
            );

            // 分类 changeType，若无变化直接返回
            ClientRiderContext.ChangeType type = classifyChange(
                    wasTransformed, oldFormId,
                    p.isTransformed(), p.state(), p.currentFormId()
            );
            if (type == null) return;

            // 只有本地玩家才处理（远端玩家的状态同步留给 UI / HUD）
            LocalPlayer local = Minecraft.getInstance().player;
            if (local == null || !local.getUUID().equals(playerId)) return;

            // 拿 driver（本地玩家在客户端侧可以正常读装备槽）
            RiderConfig config = p.riderId() != null
                    ? RiderRegistry.getRider(p.riderId())
                    : null;
            ItemStack driver = config != null
                    ? local.getItemBySlot(config.getDriverSlot())
                    : ItemStack.EMPTY;

            // 构造 context 并分派
            ClientRiderContext ctx = new ClientRiderContext(
                    local,
                    driver,
                    p.riderId(),
                    p.currentFormId(),
                    p.pendingFormId(),
                    null,   // changedItems
                    null,               // skillId
                    type
            );

            final ClientRiderContext.ChangeType finalType = type;
            ClientRiderDispatcher.dispatch(p.riderId(), handler -> {
                switch (finalType) {
                    case HENSHIN -> handler.postHenshin(ctx);
                    case SWITCH -> handler.postSwitch(ctx);
                    case UNHENSHIN -> handler.postUnhenshin(ctx);
                    case PENDING -> handler.onPending(ctx);
                }
            });
        });
    }

    // ========== 驱动器数据同步 ==========
    public static void applyDriverData(DriverDataSyncPacket p) {
        Minecraft.getInstance().execute(() -> {
            ClientDriverDataCache.setMainItems(p.playerId(), p.mainItems());
            ClientDriverDataCache.setAuxItems(p.playerId(), p.auxItems());
        });
    }

    public static void applyDriverDiff(DriverDataDiffPacket p) {
        Minecraft.getInstance().execute(() -> {
            ClientDriverDataCache.applyChanges(p.playerId(), p.changes());

            LocalPlayer local = Minecraft.getInstance().player;
            if (local == null || !local.getUUID().equals(p.playerId())) return;

            ResourceLocation riderId = ClientTransformedCache.getRiderId(p.playerId());
            RiderConfig config = riderId != null ? RiderRegistry.getRider(riderId) : null;
            if (config == null) return;
            ItemStack driver = local.getItemBySlot(config.getDriverSlot());

            for (Map.Entry<ResourceLocation, ItemStack> entry : p.changes().entrySet()) {
                ResourceLocation slotId = entry.getKey();
                ItemStack stack = entry.getValue();

                ClientRiderContext ctx = new ClientRiderContext(
                        local,
                        driver,
                        riderId,
                        ClientTransformedCache.getCurrentFormId(p.playerId()),
                        ClientTransformedCache.getPendingFormId(p.playerId()),
                        Map.of(slotId, stack),   // 只带这一条
                        null,
                        ClientRiderContext.ChangeType.DRIVER_CHANGE
                );

                // 精确分发
                if (stack.isEmpty()) {
                    ClientRiderDispatcher.dispatch(riderId, h -> h.onDriverItemExtracted(ctx));
                } else {
                    ClientRiderDispatcher.dispatch(riderId, h -> h.onDriverItemInserted(ctx));
                }
            }
        });
    }

    // ========== 技能同步 ==========
    public static void applySkill(ResourceLocation riderId, ResourceLocation skillId) {
        Minecraft.getInstance().execute(() -> {
            LocalPlayer local = Minecraft.getInstance().player;
            if (local == null) return;

            RiderConfig config = riderId != null ? RiderRegistry.getRider(riderId) : null;
            ItemStack driver = config != null
                    ? local.getItemBySlot(config.getDriverSlot())
                    : ItemStack.EMPTY;

            ClientRiderContext ctx = new ClientRiderContext(
                    local,
                    driver,
                    riderId,
                    ClientTransformedCache.getCurrentFormId(local.getUUID()),
                    ClientTransformedCache.getPendingFormId(local.getUUID()),
                    null,
                    skillId,
                    ClientRiderContext.ChangeType.SKILL
            );

            ClientRiderDispatcher.dispatch(riderId, h -> h.onSkill(ctx));
        });
    }

    // ========== 内部：状态分类 ==========
    private static @Nullable ClientRiderContext.ChangeType classifyChange(
            boolean wasTransformed, @Nullable ResourceLocation oldForm,
            boolean isTransformed, HenshinState state,
            @Nullable ResourceLocation newForm) {

        if (state == HenshinState.TRANSFORMING) {
            return ClientRiderContext.ChangeType.PENDING;
        }
        if (isTransformed && !wasTransformed) {
            return ClientRiderContext.ChangeType.HENSHIN;
        }
        if (!isTransformed && wasTransformed) {
            return ClientRiderContext.ChangeType.UNHENSHIN;
        }
        if (isTransformed && !Objects.equals(newForm, oldForm)) {
            return ClientRiderContext.ChangeType.SWITCH;
        }
        // 状态没变（比如单纯刷新），不发事件
        return null;
    }
}