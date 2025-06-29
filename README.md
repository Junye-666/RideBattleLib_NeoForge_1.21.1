# RideBattleLib 使用文档

## 概述
这是一个完全由假面骑士爱好者写出来的假面骑士代码库, 旨在让骑士+MC爱好者们可以在新版本NeoForge上玩电子CSM, 当赛博骑士
此模组允许作者们通过简单的接口实现复杂的变身逻辑，包括：
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

// 调用RiderRegistry的内置注册方式
RiderRegistry.registerRider(riderAlpha);
```

### 4. 触发变身
你可以通过以下方式触发变身：

#### a. 按键触发
按下 G 键来触发变身。你需要确保玩家装备了驱动器，并且腰带中有满足条件的物品。

#### b. 自定义动画事件
通过监听 [HenshinEvent](file://D:\Apps\Minecraft\Coding\Mod\RideBattleLib_NeoForge_1.21.1\src\main\java\com\jpigeon\ridebattlelib\core\system\event\AnimationEvent.java#L7-L29)，你可以插入自定义动画逻辑。
当在注册骑士形态, 也就是FormConfig时, 如果定义setShouldPause为true，则当触发变身后，会进入变身缓冲阶段。
```java
NeoForge.EVENT_BUS.addListener((HenshinEvent event) -> {
    // 在这里可以匹配你的骑士Id
    if (event.getRiderId().equals(TEST_RIDER_ALPHA)) {
        // 在这里实现你自己的任何逻辑
    }
});
```

### 5. 继续变身
当你的自定义动画完成后，调用以下方法继续执行变身逻辑。

```java
HenshinHelper.completeTransformation(player);
```

### 6. 神奇的事件类
0.9.3新增了一些Events, 增加了更大的扩展可能
现在可以通过SubscribeEvent实现特别简单的管理

## 示例代码
简单的示例可以参考 [ExampleRiders.java](file://D:\Apps\Minecraft\Coding\Mod\RideBattleLib_NeoForge_1.21.1\src\main\java\com\jpigeon\ridebattlelib\example\ExampleRiders.java#L28-L86)
