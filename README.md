# RideBattleLib 使用文档

## 概述
RideBattleLib 是一个为 Minecraft 1.21.1 NeoForge 设计的假面骑士变身系统 API 模组。它允许模组作者通过简单的接口实现复杂的变身逻辑，包括：
- 腰带物品存入与形态匹配
- 变身与解除变身
- 自定义动画事件
- 属性和效果应用
- 自定义变身触发

## 基础功能

### 1. 创建骑士配置 (`RiderConfig`)
在你的模组中创建一个 `RiderConfig` 来定义假面骑士的基本信息。

```java
RiderConfig riderAlpha = new RiderConfig(TEST_RIDER_ALPHA)
        .setDriverItem(Items.IRON_LEGGINGS, EquipmentSlot.LEGS) // 驱动器: 铁护腿(穿戴在腿部)
        .setTriggerType(TriggerType.KEY) // 按键触发变身
        .addSlot(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot"),
                List.of(Items.IRON_INGOT, Items.GOLD_INGOT),
                true,
                true
        ) // 核心槽位: 接受铁锭或金锭(必要槽位)
        .addSlot(
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "energy_slot"),
                List.of(Items.REDSTONE, Items.GLOWSTONE_DUST),
                false,
                false
        ); // 能量槽位: 接受红石或荧石粉(非必要)
```

### 2. 定义形态 (`FormConfig`)
定义不同形态的盔甲、属性、效果和所需物品。

```java
FormConfig alphaBaseForm = new FormConfig(
        ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_base_form"))
        .setArmor(// 设置盔甲
                Items.IRON_HELMET,
                Items.IRON_CHESTPLATE,
                null,
                Items.IRON_BOOTS
        )
        .addAttribute(// 增加生命值
                ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"),
                8.0,
                AttributeModifier.Operation.ADD_VALUE
        )
        .addAttribute(// 增加移动速度
                ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"),
                0.1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
        .addEffect(// 增加夜视效果
                MobEffects.NIGHT_VISION,
                114514,
                0,
                true
        )
        .addRequiredItem(// 要求核心槽位有铁锭
                ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "core_slot"),
                Items.IRON_INGOT
        )
        .addGrantedItem(Items.IRON_SWORD.getDefaultInstance());
```

### 3. 注册骑士
注册你创建的骑士和其形态。

```java
riderAlpha
        .addForm(alphaBaseForm)
        .addForm(alphaPoweredForm)
        .setBaseForm(alphaBaseForm.getFormId());// 设置基础形态
alphaBaseForm.setAllowsEmptyBelt(false);

// 注册骑士
RiderRegistry.registerRider(riderAlpha);
```

### 4. 触发变身
你可以通过以下方式触发变身：

#### a. 按键触发
按下 G 键来触发变身。你需要确保玩家装备了驱动器，并且腰带中有满足条件的物品。

#### b. 自定义动画事件
通过监听 [AnimationEvent](file://D:\Apps\Minecraft\Coding\Mod\RideBattleLib_NeoForge_1.21.1\src\main\java\com\jpigeon\ridebattlelib\core\system\event\AnimationEvent.java#L7-L29)，你可以插入自定义动画逻辑。

```java
NeoForge.EVENT_BUS.addListener((AnimationEvent event) -> {
    if (event.getPhase() == AnimationPhase.INIT && 
        event.getRiderId().equals(TEST_RIDER_ALPHA)) {
        // 在初始化阶段暂停变身
        event.setCanceled(true);
        HenshinHelper.pauseTransformation(event.getPlayer(), TEST_RIDER_ALPHA);
        event.getPlayer().displayClientMessage(Component.literal("[测试] 变身已暂停，请[自定义事件]继续").withStyle(ChatFormatting.YELLOW), true);
    }
});
```

### 5. 继续变身
当你的自定义动画完成后，调用以下方法继续执行变身逻辑。

```java
HenshinHelper.resumeTransformation(player, TEST_RIDER_ALPHA);
```

### 6. 形态匹配逻辑
骑士会根据腰带中的物品自动匹配到对应的形态。例如：
- 如果腰带中是 **铁锭 + 红石**，则匹配到 `baseForm`
- 如果腰带中是 **金锭 + 红石**，则匹配到 `poweredForm`

你可以通过 `config.matchForm(beltItems)` 来获取当前应该匹配的形态 ID。

### 7. 物品授予
当你成功变身后，可以通过 `grantFormItems(player, formId)` 授予玩家特定形态的物品。

```java
grantFormItems(player, matchedFormId);
```

### 8. 装备盔甲
最后，你可以通过 `equipArmor(player, form, beltItems)` 为玩家装备对应形态的盔甲。

```java
equipArmor(player, form, beltItems);
```

## 高级功能

### 1. 动画事件
你可以利用 [AnimationEvent](file://D:\Apps\Minecraft\Coding\Mod\RideBattleLib_NeoForge_1.21.1\src\main\java\com\jpigeon\ridebattlelib\core\system\event\AnimationEvent.java#L7-L29) 和 [AnimationPhase](file://D:\Apps\Minecraft\Coding\Mod\RideBattleLib_NeoForge_1.21.1\src\main\java\com\jpigeon\ridebattlelib\core\system\animation\AnimationPhase.java#L2-L4) 来控制动画流程。

- `INIT`: 初始阶段，可用于暂停变身
- `PAUSED`: 暂停阶段
- `CONTINUE`: 继续阶段，可用于恢复变身
- `FINALIZE`: 结束阶段，用于最终处理

### 2. 自定义实体
你可以在变身时创建自定义实体（如光幕），并等待玩家交互后再继续变身。

```java
createLightScreenEntity(player);
```

## 示例代码
完整示例可以参考 [ExampleRiders.java](file://D:\Apps\Minecraft\Coding\Mod\RideBattleLib_NeoForge_1.21.1\src\main\java\com\jpigeon\ridebattlelib\example\ExampleRiders.java#L28-L86)

## 总结
通过以上步骤，你可以轻松地为你的模组添加假面骑士变身系统。如果你需要更高级的功能，可以扩展 `RiderConfig` 或 `FormConfig` 类，或者订阅更多事件以进行深度定制。