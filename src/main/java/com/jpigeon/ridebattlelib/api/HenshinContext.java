package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.henshin.helper.ArmorManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.EffectAndAttributeManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.ItemManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.data.SyncManager;

/**
 * 变身上下文，方便调用方法
 */
public class HenshinContext {
    public static final ArmorManager ARMOR = ArmorManager.getInstance();
    public static final EffectAndAttributeManager EFFECTS = EffectAndAttributeManager.getInstance();
    public static final ItemManager ITEMS = ItemManager.getInstance();
    public static final SyncManager DATA_SYNC = SyncManager.getInstance();
}
