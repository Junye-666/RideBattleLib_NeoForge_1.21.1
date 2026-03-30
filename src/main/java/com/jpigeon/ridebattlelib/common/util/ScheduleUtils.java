package com.jpigeon.ridebattlelib.common.util;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScheduleUtils {
    private static final ScheduleUtils INSTANCE = new ScheduleUtils();
    private final ConcurrentLinkedQueue<ScheduledTask> tasks = new ConcurrentLinkedQueue<>();
    private final Map<UUID, ScheduledTask> taskById = new ConcurrentHashMap<>();

    public static ScheduleUtils getInstance() { return INSTANCE; }

    /**
     * 调度一个一次性延迟任务
     * @param ticks 延迟 tick 数
     * @param callback 任务回调
     * @return 任务 ID，可用于取消
     */
    public UUID scheduleTask(int ticks, Runnable callback) {
        ScheduledTask task = new ScheduledTask(ticks, callback);
        tasks.add(task);
        taskById.put(task.id, task);
        return task.id;
    }

    /**
     * 调度一个周期性任务，重复执行直到取消
     * @param intervalTicks 执行间隔 tick 数
     * @param callback 任务回调
     * @return 任务 ID，可用于取消
     */
    public UUID scheduleRepeatingTask(int intervalTicks, Runnable callback) {
        ScheduledTask task = new ScheduledTask(intervalTicks, callback, true);
        tasks.add(task);
        taskById.put(task.id, task);
        return task.id;
    }

    /**
     * 取消一个任务
     * @param taskId 任务 ID
     * @return 是否成功取消（任务存在且尚未执行）
     */
    public boolean cancelTask(UUID taskId) {
        ScheduledTask task = taskById.remove(taskId);
        if (task != null) {
            return tasks.remove(task);
        }
        return false;
    }

    /**
     * 取消所有任务
     */
    public void cancelAllTasks() {
        tasks.clear();
        taskById.clear();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tick();
    }

    private void tick() {
        // 使用迭代器遍历并移除已完成的非周期性任务，周期性任务保留
        tasks.removeIf(task -> {
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                try {
                    task.callback.run();
                } catch (Exception e) {
                    RideBattleLib.LOGGER.error("ScheduleUtils 任务出错: {}", e.getMessage());
                }
                if (task.repeating) {
                    // 周期性任务重置计数
                    task.remainingTicks = task.interval;
                    return false; // 不移除
                } else {
                    taskById.remove(task.id);
                    return true; // 一次性任务移除
                }
            }
            return false;
        });
    }

    /**
     * 内部任务类
     */
    private static class ScheduledTask {
        final UUID id;
        final int interval;
        int remainingTicks;
        final Runnable callback;
        final boolean repeating;

        ScheduledTask(int ticks, Runnable callback) {
            this(ticks, callback, false);
        }

        ScheduledTask(int ticks, Runnable callback, boolean repeating) {
            this.id = UUID.randomUUID();
            this.interval = ticks;
            this.remainingTicks = ticks;
            this.callback = callback;
            this.repeating = repeating;
        }
    }
}
