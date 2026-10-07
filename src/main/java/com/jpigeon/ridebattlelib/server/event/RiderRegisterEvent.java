package com.jpigeon.ridebattlelib.server.event;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

/**
 * 骑士 Config 进入 RIDER 表之后、任何全局索引建立之前触发。
 * <p>
 * 允许外部在这里修改：
 * <ul>
 *   <li>{@link RiderConfig#setMainDriverItem}</li>
 *   <li>{@link RiderConfig#getForms()} 增删改（会被后续 FormRegisterEvent 遍历到）</li>
 *   <li>槽位定义 {@link RiderConfig#addMainDriverSlot} / {@link RiderConfig#addAuxDriverSlot}</li>
 * </ul>
 * 修改之后本模组会用修改后的config 建立：
 * {@code DRIVER_ITEM_INDEX}、form 级注册表、{@code RiderArmorRegistry}。
 * <p>
 * 在 {@code NeoForge.EVENT_BUS} 上派发。
 */
public class RiderRegisterEvent extends Event {

    private final RiderConfig config;

    public RiderRegisterEvent(RiderConfig config) {
        this.config = config;
    }

    public RiderConfig getConfig() {
        return config;
    }

    public ResourceLocation getRiderId() {
        return config.getRiderId();
    }

    /**
     * 便捷方法；等价于 {@code getConfig().addForm(form)}。
     */
    public void addForm(FormConfig form) {
        config.addForm(form);
    }
}