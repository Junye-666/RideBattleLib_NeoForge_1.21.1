package com.jpigeon.ridebattlelib.server.event;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

/**
 * 单个 form 即将被写入全局 FORM 表 / FORM_TO_RIDERS / 技能表时触发。
 * <p>
 * 相比 {@link RiderRegisterEvent} 提供 form 级粒度。触发顺序：
 * <pre>registerFormForRider → FormRegisterEvent → flushPendingSkills</pre>
 * 因此外部可以在事件里修改 form 的 skill、armor、attribute 等内容，
 * 之后再被 {@link com.jpigeon.ridebattlelib.common.registry.RiderArmorRegistry}
 * 收录。
 */
public class FormRegisterEvent extends Event {

    private final RiderConfig rider;
    private final FormConfig form;

    public FormRegisterEvent(RiderConfig rider, FormConfig form) {
        this.rider = rider;
        this.form = form;
    }

    public RiderConfig getRider() {
        return rider;
    }

    public FormConfig getForm() {
        return form;
    }

    public ResourceLocation getRiderId() {
        return rider.getRiderId();
    }

    public ResourceLocation getFormId() {
        return form.getFormId();
    }
}