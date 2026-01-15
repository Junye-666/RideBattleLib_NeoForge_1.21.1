package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CountdownManager {
    private static final CountdownManager INSTANCE = new CountdownManager();
    private final List<CountdownTask> activeTasks = new ArrayList<>();

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

        Iterator<CountdownTask> iterator = activeTasks.iterator();
        int executedCount = 0;

        while (iterator.hasNext()) {
            CountdownTask task = iterator.next();
            task.remainingTicks--;

            if (task.remainingTicks <= 0) {
                try {
                    if (Config.DEBUG_MODE.get()) {
                        RideBattleLib.LOGGER.debug("执行任务, 剩余任务数: {}", activeTasks.size() - 1);
                    }
                    task.callback.run();
                    executedCount++;
                } catch (Exception e) {
                    RideBattleLib.LOGGER.error("任务执行失败: {}", e.getMessage());
                } finally {
                    iterator.remove();
                }
            }
        }

        if (executedCount > 0) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("本轮执行了 {} 个任务", executedCount);
            }
        }
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
