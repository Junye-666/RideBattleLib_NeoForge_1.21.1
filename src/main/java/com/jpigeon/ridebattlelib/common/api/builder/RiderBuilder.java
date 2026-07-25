package com.jpigeon.ridebattlelib.common.api.builder;

import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * 一站式骑士构建器 - 快速创建并注册一个完整的骑士
 *
 */
public class RiderBuilder {
    private final ResourceLocation riderId;
    private final RiderConfig config;
    private final Map<String, FormBuilder> formBuilders = new LinkedHashMap<>();
    private final List<FormConfig> directForms = new ArrayList<>();
    private String baseFormPath;

    private RiderBuilder(ResourceLocation riderId) {
        this.riderId = riderId;
        this.config = new RiderConfig(riderId);
    }

    /**
     * 创建一个新的骑士构建器
     */
    public static RiderBuilder create(ResourceLocation riderId) {
        return new RiderBuilder(riderId);
    }

    // ========== 驱动器配置 ==========

    public RiderBuilder driver(Item item) {
        config.setMainDriverItem(item);
        return this;
    }

    public RiderBuilder driver(Item item, EquipmentSlot slot) {
        config.setMainDriverItem(item, slot);
        return this;
    }

    public RiderBuilder auxDriver(Item item) {
        config.setAuxDriverItem(item);
        return this;
    }

    public RiderBuilder auxDriver(Item item, EquipmentSlot slot) {
        config.setAuxDriverItem(item, slot);
        return this;
    }

    // ========== 槽位配置 ==========

    public RiderBuilder slot(ResourceLocation slotId, List<Item> allowedItems, boolean required, boolean replace) {
        config.addMainDriverSlot(slotId, allowedItems, required, replace);
        return this;
    }

    public RiderBuilder auxSlot(ResourceLocation slotId, List<Item> allowedItems, boolean required, boolean replace) {
        config.addAuxDriverSlot(slotId, allowedItems, required, replace);
        return this;
    }

    // ========== 形态构建 ==========

    /**
     * 开始构建一个形态（使用形态名称，自动拼接命名空间）
     */
    public FormBuilder form(String formPath) {
        return new FormBuilder(this, ResourceLocation.fromNamespaceAndPath(riderId.getNamespace(), formPath));
    }

    /**
     * 开始构建一个形态（使用完整 ResourceLocation）
     */
    public FormBuilder form(ResourceLocation formId) {
        return new FormBuilder(this, formId);
    }

    /**
     * 直接添加一个已经构建好的 FormConfig 到当前骑士中
     */
    public RiderBuilder form(FormConfig formConfig) {
        this.directForms.add(formConfig);
        return this;
    }

    // ========== 骑士全局设置 ==========

    public RiderBuilder baseForm(String formPath) {
        this.baseFormPath = formPath;
        return this;
    }

    public RiderBuilder baseForm(ResourceLocation formId) {
        this.baseFormPath = formId.getPath();
        return this;
    }

    public RiderBuilder allowDynamicForms(boolean allow) {
        config.setAllowDynamicForms(allow);
        return this;
    }

    public RiderBuilder triggerItem(Item item) {
        config.setTriggerItem(item);
        return this;
    }

    // ========== 基础属性/效果（整个骑士生效） ==========

    public RiderBuilder baseAttribute(ResourceLocation attributeId, double amount) {
        config.addBaseAttribute(attributeId, amount, AttributeModifier.Operation.ADD_VALUE);
        return this;
    }

    public RiderBuilder baseAttribute(ResourceLocation attributeId, double amount, AttributeModifier.Operation operation) {
        config.addBaseAttribute(attributeId, amount, operation);
        return this;
    }

    public RiderBuilder baseEffect(Holder<MobEffect> effect, int amplifier) {
        config.addBaseEffect(effect, amplifier);
        return this;
    }

    public RiderBuilder baseEffect(Holder<MobEffect> effect, int duration, int amplifier, boolean hideParticles) {
        config.addBaseEffect(effect, duration, amplifier, hideParticles);
        return this;
    }

    // ========== 构建 ==========

    /**
     * 构建并自动注册骑士到 RiderRegistry
     */
    public RiderConfig buildAndRegister() {
        RiderConfig config = this.build();
        RiderRegistry.registerRider(config);
        return config;
    }

    /**
     * 构建但不注册，返回 RiderConfig（用于手动控制注册时机）
     */
    public RiderConfig build() {
        // 构建链式形态
        Map<String, FormConfig> builtForms = new HashMap<>();
        for (Map.Entry<String, FormBuilder> entry : formBuilders.entrySet()) {
            FormConfig form = entry.getValue().build();
            config.addForm(form);
            builtForms.put(entry.getKey(), form);
        }

        // 合并独立构建的形态（新增）
        for (FormConfig form : directForms) {
            config.addForm(form);
            builtForms.put(form.getFormId().getPath(), form);
        }

        // 验证基础形态
        if (baseFormPath == null || !builtForms.containsKey(baseFormPath)) {
            throw new IllegalStateException(
                    "Base form '" + baseFormPath + "' not found! Available forms: " + builtForms.keySet()
            );
        }
        config.setBaseForm(builtForms.get(baseFormPath).getFormId());

        // 验证驱动器
        if (config.getDriverItem() == null || config.getDriverItem() == Items.AIR) {
            throw new IllegalStateException("Driver item is required! Call .driver() before building.");
        }

        return config;
    }

    // ========== 内部方法 ==========

    void addFormBuilder(String path, FormBuilder builder) {
        formBuilders.put(path, builder);
    }
}