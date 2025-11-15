# RideBattleLib - 在MC中变身假面骑士！
**简体中文** | [English](README_en.md)

"Cyclone! Joker! さあ、お前の罪を数えろ！"

现在，在Minecraft中喊出你的变身台词吧！

## 🎯 概述
欢迎来到 RideBattleLib - 一个为MC高版本（1.21.1/1.21.8，NeoForge）打造的假面骑士变身系统库！

在Minecraft中插入道具，激活驱动器，喊出"Henshin！" ！

无论你是想重现Build的满装瓶系统，还是创造 **☆全☆新☆的☆骑☆士☆系☆统** ，RideBattleLib都能让你轻松实现！

此页为1.21.1, [1.21.8在这](https://github.com/Junye-666/RideBattleLib_NeoForge_1.21.8)

## ✨ 核心功能
### 🧩 1. 骑士变身系统
- **完整变身状态管理** - 待机→变身中→变身完成

```java
// 创建骑士配置
public static final ResourceLocation MY_RIDER_ID =
        ResourceLocation.fromNamespaceAndPath(MODID, "kamen_rider_demo");

public static final RiderConfig MY_RIDER = new RiderConfig(MY_RIDER_ID)
        .setMainDriverItem(ModItems.DRIVER_ITEM.get(), EquipmentSlot.LEGS)
        .setAuxDriverItem(ModItems.AUX_DRIVER.get(), EquipmentSlot.OFFHAND)
        .setTriggerItem(ModItems.TRIGGER_ITEM.get())
        .setAllowDynamicForms(true);
```

### ⚡ 2. 驱动器系统
- **双驱动器系统** - 主驱动器 + 辅助驱动器

```java
// 以以上骑士为例, 规定其驱动器接受的物品
RiderConfig MY_RIDER = new RiderConfig(MY_RIDER_ID)
                // 添加主驱动器槽位
                .addMainDriverSlot(
                        ResourceLocation.fromNamespaceAndPath(MODID, "core_slot"),
                        List.of(
                                ModItems.CORE_ITEM_1.get(),
                                ModItems.CORE_ITEM_2.get(),
                                ModItems.CORE_ITEM_3.get()
                        ),
                        true,  // 是否必要
                        true   // 内容是否可被直接替换
                )
                // 添加辅助驱动器槽位  
                .addAuxDriverSlot(
                        ResourceLocation.fromNamespaceAndPath(MODID, "aux_slot"),
                        List.of(ModItems.AUX_ITEM.get()),
                        false,
                        false
                );
```
### 🔮 3. 形态系统
- **预设形态** + **动态形态生成**

```java
// 创建预设形态
FormConfig baseForm = new FormConfig(
                ResourceLocation.fromNamespaceAndPath(MODID, "base_form")
        )
                .setArmor(
                        ModItems.RIDER_HELMET.get(),
                        ModItems.RIDER_CHESTPLATE.get(),
                        null,  // 保持驱动器不被替换
                        ModItems.RIDER_BOOTS.get()
                )
                .addRequiredItem(
                        ResourceLocation.fromNamespaceAndPath(MODID, "core_slot"),
                        ModItems.CORE_ITEM_1.get()
                )
                .addEffect(MobEffects.DAMAGE_BOOST, 0)
                .addSkill(ResourceLocation.fromNamespaceAndPath(MODID, "rider_kick"));

// 注册到骑士
MY_RIDER.addForm(baseForm);
```
### 🚀 4. 动态形态系统
- 可能的形态组合数量太多, 注册不过来怎么办? (就针对Build和OOO来的)

- 自动根据驱动器物品生成形态！
```java
// 注册动态形态映射
private static void registerDynamicMappings() {
    // 物品到盔甲的映射
    DynamicFormConfig.registerItemArmor(
        ModItems.RABBIT_BOTTLE.get(), 
        EquipmentSlot.HEAD, 
        ModItems.RABBIT_ARMOR.get()
    );
    
    // 物品到效果的映射
    DynamicFormConfig.registerItemEffect(
        ModItems.RABBIT_BOTTLE.get(),
        MobEffects.JUMP,
        114514,  // 持续时间
        1,       // 等级
        false    // 是否隐藏粒子
    );
    
    // 注册底衣配置
    DynamicFormConfig.registerRiderUndersuit(
        MY_RIDER_ID,
        ModItems.UNDERSUIT_HELMET.get(),
        ModItems.UNDERSUIT_CHESTPLATE.get(),
        ModItems.UNDERSUIT_LEGGINGS.get(), 
        ModItems.UNDERSUIT_BOOTS.get()
    );
}
```
### 💥 5. 吃瘪系统
- 生命值过低时强制解除变身 + 冷却机制
### 🎭 6. 事件系统
- 完整的变身生命周期事件
```java
// 变身事件
@SubscribeEvent
public static void onHenshin(HenshinEvent.Pre event) {
    // 变身前的逻辑（动画、音效等）
    Player player = event.getPlayer();
    player.level().playSound(null, player, 
        SoundEvents.LIGHTNING_BOLT_THUNDER, 
        SoundSource.PLAYERS, 1.0F, 1.0F
    );
}

@SubscribeEvent  
public static void onHenshin(HenshinEvent.Post event) {
    // 变身完成后的逻辑
    RiderManager.playPublicSound(event.getPlayer(), SoundEvents.PLAYER_LEVELUP);
}

// 形态切换事件
@SubscribeEvent
public static void onFormSwitch(FormSwitchEvent.Post event) {
    Player player = event.getPlayer();
    player.displayClientMessage(
        Component.literal("形态切换: " + event.getNewFormId()),
        true
    );
}
```
### ⚡ 7. 技能系统
- 多技能轮转支持
```java
// 注册技能
SkillSystem.registerSkillName(
    ResourceLocation.fromNamespaceAndPath(MODID, "rider_kick"), // 技能辨识用ID
    Component.translatable("skill.mymod.rider_kick") // 支持多语言
);

// 监听技能触发
@SubscribeEvent  
public static void onSkillTrigger(SkillEvent.Post event) {
    if (event.getSkillId().getPath().equals("rider_kick")) {
        Player player = event.getPlayer();
        // 执行骑士踢逻辑
        performRiderKick(player);
    }
}

private static void performRiderKick(Player player) {
    // 实现骑士踢技能
    Vec3 lookVec = player.getLookAngle();
    player.setDeltaMovement(lookVec.x * 2, 1.0, lookVec.z * 2);
    player.hurtMarked = true;
    
    // 对周围敌人造成伤害
    AABB area = player.getBoundingBox().inflate(3);
    // ... 伤害逻辑
}
```
### 🌐 8. 网络同步
- 完善的客户端-服务端同步
```java
// 手动同步状态（如需要）
if (player instanceof ServerPlayer serverPlayer) {
    RiderManager.syncClientState(serverPlayer);
    RiderManager.syncDriverData(serverPlayer); 
    RiderManager.syncHenshinState(serverPlayer);
}
```
## 🎮 玩家操作指南
### 基本操作流程：

装备驱动器 - 将驱动器物品装备到指定槽位

插入道具 - 右键物品插入驱动器槽位

触发变身 - 按 G 键或使用触发物品

切换形态 - 更换驱动器中的物品

使用技能 - 按 R 键触发当前技能，Shift + R 切换技能

解除变身 - 按 V 键手动解除，或生命值过低时自动吃瘪

### 默认按键绑定：

**G** - 触发驱动器

**V** - 解除变身

**R** - 使用技能 / Shift + R 切换技能

**X** - 返还所有驱动器物品

## ⚙️ 进阶功能
### 变身流程控制
```java
FormConfig advancedForm = new FormConfig(FORM_ID)
        .setShouldPause(true)  // 启用变身暂停（等待动画完成）
        .setTriggerType(TriggerType.ITEM);  // 触发类型：KEY/ITEM/AUTO
```
然后通过监听HenshinEvent/FormSwitchEvent：
```java
@SubscribeEvent
public static void onHenshin(HenshinEvent.Pre event) {
    // 变身动画, 特效, 音效, 特殊逻辑
}

@SubscribeEvent
public static void onFormSwitch(FormSwitchEvent.Pre event) {
    // 形态切换动画, 特效, 音效, 特殊逻辑
}

// 在动画完成后继续变身
    // 调用方法
    RiderManager.completeHenshin(player);
```
### 形态覆盖
```java
@SubscribeEvent
public static void onFormOverride(FormOverrideEvent event) {
    // 强制锁定特定形态
    if (event.getPlayer().getHealth() < 10.0f) {
        event.setOverrideForm(ResourceLocation.fromNamespaceAndPath(MODID, "emergency_form"));
    }
}
```
### 快捷API使用
```java
// 使用 RiderManager 快捷方法
RiderManager.transform(player);                    // 尝试变身
RiderManager.unTransform(player);                  // 解除变身
RiderManager.switchForm(player, newFormId);        // 切换形态
RiderManager.isTransformed(player);                // 检查变身状态
RiderManager.getDriverItems(player);               // 获取驱动器物品
RiderManager.penaltyUntransform(player);           // 强制吃瘪解除
```
## 🧪 示例
```java
// 在模组初始化中调用
ExampleBasic.init();        // 基础骑士系统
ExampleDynamicForm.init();  // 动态形态系统
```
# 📦 安装与使用
## 🌟 为什么选择RideBattleLib

✅ 专门为假面骑士玩法设计

✅ 完整的变身管理

✅ 动态形态生成黑科技(误

✅ 丰富的扩展事件

✅ 吃瘪系统 (bushi

✅ 持续更新维护

### 对于模组开发者：
- 🚀 快速上手 - 10分钟创建第一个骑士
- 🎨 高度可定制 - 从驱动器到技能全可配置
- 🔧 稳定可靠 - 完整的错误处理和网络同步

### 对于玩家：
- 🎮 沉浸体验 - 完整的变身流程和特效
- ⚡ 流畅操作 - 优化的性能和响应速度

### 🐛 问题排查
常见问题：
- 变身不生效 - 检查驱动器槽位配置和必需物品
- 形态不匹配 - 验证物品映射和槽位定义
- 网络不同步 - 确保正确调用同步方法
- 启用调试模式：在Config页面中打开Debug Mode

"变身！" - 现在就在你的模组中实现这句经典台词吧！

RideBattleLib © 2025 JPigeon
在 MIT 许可证 下开源 - 自由地创造你的骑士世界！

注: 在DeepSeek帮助下写的README.md
