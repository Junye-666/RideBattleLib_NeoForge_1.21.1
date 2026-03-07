package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CountdownManager {
    private static final CountdownManager INSTANCE = new CountdownManager();
    private final Queue<CountdownTask> activeTasks = new ConcurrentLinkedDeque<>();

    public static CountdownManager getInstance() {
        return INSTANCE;
    }

    // 添加调试日志的方法
    public void scheduleTask(int ticks, Runnable callback) {
        CountdownTask task = new CountdownTask(ticks, callback);
        activeTasks.add(task);
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("任务已安排: {} ticks后执行, 当前活跃任务数: {}", ticks, activeTasks.size());
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tick();
    }

    public void tick() {
        if (activeTasks.isEmpty()) return;

        // 使用 removeIf 结合 lambda，一行搞定遍历、检查和删除
        activeTasks.removeIf(task -> {
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                try {
                    task.callback.run();
                } catch (Exception e) {
                    RideBattleLib.LOGGER.error("任务执行失败: {}", e.getMessage());
                }
                return true; // 返回 true 表示从队列中移除
            }
            return false;
        });
    }

    private static class CountdownTask {
        int remainingTicks;
        final Runnable callback;

        CountdownTask(int ticks, Runnable callback) {
            this.remainingTicks = ticks;
            this.callback = callback;
        }
    }
}
