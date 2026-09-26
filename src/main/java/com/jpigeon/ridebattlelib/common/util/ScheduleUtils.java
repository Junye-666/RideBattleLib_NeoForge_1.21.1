package com.jpigeon.ridebattlelib.common.util;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ScheduleUtils {
    private static final ScheduleUtils INSTANCE = new ScheduleUtils();

    private final ConcurrentLinkedQueue<ScheduledTask> clientTasks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ScheduledTask> serverTasks = new ConcurrentLinkedQueue<>();
    private final Map<UUID, ScheduledTask> taskById = new ConcurrentHashMap<>();

    private ScheduleUtils() {
    }

    public static ScheduleUtils getInstance() {
        return INSTANCE;
    }

    // ===== 调度 =====

    /**
     * 自动判定逻辑侧
     */
    public UUID scheduleTask(int ticks, Runnable callback) {
        return scheduleTask(ticks, callback, currentSide());
    }

    public UUID scheduleTask(int ticks, Runnable callback, Side side) {
        ScheduledTask task = new ScheduledTask(ticks, callback, false);
        (side == Side.CLIENT ? clientTasks : serverTasks).add(task);
        taskById.put(task.id, task);
        return task.id;
    }

    public UUID scheduleRepeatingTask(int intervalTicks, Runnable callback, Side side) {
        ScheduledTask task = new ScheduledTask(intervalTicks, callback, true);
        (side == Side.CLIENT ? clientTasks : serverTasks).add(task);
        taskById.put(task.id, task);
        return task.id;
    }

    /**
     * 默认按当前侧
     */
    public UUID scheduleRepeatingTask(int intervalTicks, Runnable callback) {
        return scheduleRepeatingTask(intervalTicks, callback, currentSide());
    }

    public boolean cancelTask(UUID taskId) {
        ScheduledTask task = taskById.remove(taskId);
        if (task == null) return false;
        return clientTasks.remove(task) | serverTasks.remove(task);
    }

    public void cancelAllTasks() {
        clientTasks.clear();
        serverTasks.clear();
        taskById.clear();
    }

    // ===== Tick =====

    public void tickClient() {
        tickQueue(clientTasks);
    }

    public void tickServer() {
        tickQueue(serverTasks);
    }

    private void tickQueue(ConcurrentLinkedQueue<ScheduledTask> queue) {
        queue.removeIf(task -> {
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                try {
                    task.callback.run();
                } catch (Exception e) {
                    RideBattleLib.LOGGER.error("ScheduleUtils 任务出错: {}", e.getMessage(), e);
                }
                if (task.repeating) {
                    task.remainingTicks = task.interval;
                    return false;
                } else {
                    taskById.remove(task.id);
                    return true;
                }
            }
            return false;
        });
    }

    // ===== 侧别判定 =====

    public enum Side {CLIENT, SERVER}

    private static Side currentSide() {
        if (FMLEnvironment.dist.isDedicatedServer()) return Side.SERVER;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isSameThread()) return Side.CLIENT;
        } catch (Throwable ignored) {
        }
        return Side.SERVER;
    }

    // ===== 内部 =====

    private static class ScheduledTask {
        final UUID id;
        final int interval;
        int remainingTicks;
        final Runnable callback;
        final boolean repeating;

        ScheduledTask(int ticks, Runnable callback, boolean repeating) {
            this.id = UUID.randomUUID();
            this.interval = ticks;
            this.remainingTicks = ticks;
            this.callback = callback;
            this.repeating = repeating;
        }
    }
}