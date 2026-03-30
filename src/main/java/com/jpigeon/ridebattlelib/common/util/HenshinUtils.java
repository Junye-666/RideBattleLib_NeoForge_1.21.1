package com.jpigeon.ridebattlelib.common.util;

import com.jpigeon.ridebattlelib.common.config.DynamicFormConfig;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import com.jpigeon.ridebattlelib.server.system.helper.ArmorManager;
import com.jpigeon.ridebattlelib.server.system.helper.DynamicHenshinManager;
import com.jpigeon.ridebattlelib.server.system.helper.EffectAndAttributeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class HenshinUtils {
    /**
     * 判断玩家是否处于变身状态（服务端）
     * 客户端不应调用此方法，应使用缓存
     */
    public static boolean isTransformed(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        return data.isTransformed();
    }

    /**
     * 保存变身快照（将当前装备和驱动器内容保存到附件）
     */
    public static void saveTransformedSnapshot(Player player, RiderConfig config, ResourceLocation formId,
                                               Map<EquipmentSlot, ItemStack> originalGear,
                                               Map<ResourceLocation, ItemStack> driverSnapshot) {
        if (config == null) return;

        // 过滤空栈
        Map<EquipmentSlot, ItemStack> filteredGear = new EnumMap<>(EquipmentSlot.class);
        originalGear.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                filteredGear.put(slot, stack.copy());
            }
        });

        Map<ResourceLocation, ItemStack> filteredDriver = new HashMap<>();
        driverSnapshot.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                filteredDriver.put(slot, stack.copy());
            }
        });

        HenshinSessionData session = new HenshinSessionData(
                config.getRiderId(),
                formId,
                filteredGear,
                filteredDriver
        );

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.startHenshinSession(session);
    }

    /**
     * 恢复变身状态（用于重连等场景）
     */
    public static void restoreTransformedState(Player player, HenshinSessionData data) {
        if (data == null) return;

        ResourceLocation formId = data.formId();

        // 恢复原始装备
        ArmorManager.getInstance().restoreOriginalGear(player, data);

        // 重新应用形态盔甲
        FormConfig formConfig = RiderRegistry.getForm(player, formId);
        if (formConfig != null) {
            if (formConfig instanceof DynamicFormConfig dynamic) {
                DynamicHenshinManager.applyDynamicArmor(player, dynamic);
            } else {
                ArmorManager.getInstance().equipArmor(player, formConfig);
            }
        }

        // 重新应用属性效果
        EffectAndAttributeManager.getInstance().applyAttributesAndEffects(player, formId);
    }

    /**
     * 清除变身数据（解除时调用）
     */
    public static void clearTransformedData(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.endHenshinSession();
    }

    /**
     * 获取当前变身数据
     */
    @Nullable
    public static HenshinSessionData getSessionData(Player player) {
        return player.getData(RiderAttachments.RIDER_DATA).getSessionData();
    }
}
