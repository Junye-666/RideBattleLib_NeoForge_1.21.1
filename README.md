# RideBattleLib – 在 Minecraft 中变身假面骑士！

**简体中文** | [English](README_en.md)

> “Cyclone! Joker! さあ、お前の罪を数えろ！”

RideBattleLib 是一个为 **Minecraft NeoForge 1.21.1 / 1.21.11 / 26.1 +** 打造的 **假面骑士变身系统 API**。  
提供完整的驱动器、形态、技能、事件及网络同步框架，让开发者能快速构建属于自己的骑士模组。

---

## 🎯 核心特性

- **一键式骑士构建器** – 使用 `RiderBuilder` + `FormBuilder` 快速定义骑士与形态。
- **双驱动器系统** – 主驱动器 + 辅助驱动器，支持自定义槽位与物品规则。
- **动态形态生成** – 根据驱动器内的物品自动组合盔甲、效果、授予物品，并用 `DynamicMappingBuilder` 注册映射。
- **事件驱动的生命周期** – 从变身、切换形态、技能触发到吃瘪，全部可通过事件介入。
- **完整的网络同步** – 服务端 ↔ 客户端状态自动同步，无需额外操心。
- **内置技能轮转** – 支持多技能绑定、冷却、切换及触发。
- **吃瘪系统** – 血量过低时强制解除变身并进入冷却，可自定义阈值与惩罚效果。

---

## 🚀 快速上手

### 1. 创建你的第一个骑士（使用 Builder）

```java
RiderConfig rider = RiderBuilder.create(
        ResourceLocation.fromNamespaceAndPath("mymod", "kamen_rider_zero_one")
    )
    .driver(ModItems.ZERO_ONE_DRIVER.get(), EquipmentSlot.LEGS)
    .auxDriver(ModItems.AUX_DRIVER.get(), EquipmentSlot.OFFHAND)
    .slot(
        ResourceLocation.fromNamespaceAndPath("mymod", "core_slot"),
        List.of(Items.IRON_INGOT, Items.GOLD_INGOT),
        true,  // 必需
        true   // 允许替换
    )
    .form(ResourceLocation.fromNamespaceAndPath("mymod", "rising_hopper"))
        .armor(
            ModItems.RISING_HELMET.get(),
            ModItems.RISING_CHEST.get(),
            null,   // 保留驱动器槽位
            ModItems.RISING_BOOTS.get()
        )
        .requiredItem(
            ResourceLocation.fromNamespaceAndPath("mymod", "core_slot"),
            Items.IRON_INGOT
        )
        .effect(MobEffects.JUMP, 1)
        .skill("rider_kick", Component.literal("骑士踢"), 30)
        .end()
    .baseForm("rising_hopper")
    .allowDynamicForms(true)
    .buildAndRegister();  // 自动注册
```
### 2. 动态形态映射（使用 DynamicMappingBuilder）
```java
DynamicMappingBuilder.forRider(rider.getRiderId())
    .armor(Items.DIAMOND, EquipmentSlot.HEAD, Items.DIAMOND_HELMET)
    .effect(Items.DIAMOND, MobEffects.DAMAGE_BOOST, 1200, 1, false)
    .grantedItem(Items.DIAMOND, Items.DIAMOND_SWORD)
    .undersuit(
        Items.LEATHER_HELMET,
        Items.LEATHER_CHESTPLATE,
        null,
        Items.LEATHER_BOOTS
    )
    .register();  // 注册所有映射
```
### 3. 使用 RideBattleAPI 快捷控制
```java
// 在任意事件或方法中
Player player = ...;
RideBattleAPI.transform(player);          // 尝试变身
RideBattleAPI.unTransform(player);        // 解除
RideBattleAPI.switchForm(player, newFormId);
RideBattleAPI.triggerCurrentSkill(player);
boolean isTransformed = RideBattleAPI.isTransformed(player);
Map<ResourceLocation, ItemStack> items = RideBattleAPI.getDriverItems(player);
```
## 📚 完整文档与示例
进阶教程 / 完整 API 使用 → [GitHub Wiki](https://github.com/Junye-666/RideBattleLib_NeoForge_1.21.1/wiki)（包含事件监听、自定义策略、网络同步等）

示例：ExampleBasic 与 ExampleDynamicForm 类展示了基础用法与动态形态。

⚙️ 配置选项
模组提供了通用配置文件（ridebattlelib-common.toml），可调整：

吃瘪触发阈值、冷却时间、爆炸威力、击退强度

按键防抖动延迟

调试模式开关

## 🤝 贡献与许可
开源协议：MIT License

欢迎提交 Issue 或 PR 到 GitHub 仓库

现在就创建属于你自己的骑士世界吧！

“变身！” 

注: 在DeepSeek帮助下写的README.md
