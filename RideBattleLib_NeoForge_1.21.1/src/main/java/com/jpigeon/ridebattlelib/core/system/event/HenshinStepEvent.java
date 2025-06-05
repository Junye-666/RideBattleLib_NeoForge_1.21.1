// 新增分阶段事件接口
public interface HenshinStepEvent {
    boolean isPaused();
    void resume();
}