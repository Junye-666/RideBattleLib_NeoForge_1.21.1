# RideBattleLib - 让MC变身假面骑士！
"Cyclone! Joker! さあ、お前の罪を数えろ！"
现在，在Minecraft中喊出你的变身台词吧！

🎯 概述
欢迎来到 RideBattleLib - 一个为MC高版本（1.21.1，NeoForge）打造的假面骑士变身系统库！想象一下：
在Minecraft中插入道具，转动腰带，喊出"Henshin！" - 这一切都成为可能！
无论你是想重现Build的满装瓶系统，还是创造☆全☆新☆的☆骑☆士☆系统，RideBattleLib都能让你轻松实现！

## ✨ 核心功能
### 🧩 1. 骑士变身系统
定义驱动器物品（腰带）

创建变身槽位系统

多种变身触发方式：按键、物品、自动

完整变身状态管理（待机→变身中→变身完成）

```java
// 创建骑士驱动器 (当然得你自己先注册一个盔甲物品啦)
RiderConfig myRider = new RiderConfig(MY_RIDER_ID)
.setDriverItem(ModItems.ANY_DRIVER.get(), EquipmentSlot.LEGS) //戴在腿部的驱动器 
.setAuxDriverItem(ModItems.ANY_AUX_DRIVER.get(), EquipmentSlot.OFFHAND) // 拿在副手的辅助驱动器(如剑的融合进化器)
        ; // 务必注意冒号的位置, 别写早了导致错误
```

### ⚡ 2. 腰带系统
物品插入/取出机制

槽位验证系统

主驱动器+辅助驱动器双系统

物品返还功能（一键清空腰带）

```java
// 以以上骑士为例, 规定其腰带接受的物品
RiderConfig myRider = new RiderConfig(MY_RIDER_ID)
        .addDriverSlot( // 添加一个主驱动器上的槽位
                SOME_SLOT, // 槽位的ResourceLocation(这里默认在前面定义过, 这里直接调用)
                List.of( // 一个槽位可以接纳多个不同的物品
                        ModItems.ITEM_1.get(), // 可以是1
                        ModItems.ITEM_2.get(),  // 可以是2
                        ModItems.ITEM_3.get() // 可以是3
                        // 理论上无限
                ),
                true, // 是否必要 (没了就匹配不到形态了, 比如加布, 你不放东西进去当然没法变身)
                true) // 是否可被直接替换 (手感问题: 槽位装了东西后右键是否自动把物品替换了, false时就是得先手动按X键)
        
        .addDriverSlot( // 再来一个驱动器上槽位
                ANOTHER_SLOT,
                List.of(
                        //这里省略
                        ),
                false,
                false)
                
        .addAuxDriverSlot( // 添加一个辅助驱动器槽位
                // 与驱动器槽位一致, 所以省略
                        )
        ;
```
### 🔮 3. 形态系统
预设形态配置（基础形态，强化形态等）

动态形态生成（根据腰带物品自动创建新形态！）

形态专属属性/效果加成

形态切换功能

```java
// 为某个骑士创建形态
FormConfig anyForm = new FormConfig(ANY_FORM_ID)
.setArmor( // 为这个形态绑定盔甲 (当然也得你自己注册盔甲物品啦)
        ModItems.RIDER_HELMET.get(),
        ModItems.RIDER_CHESTPLATE.get(),
        null, // 这里为null, 如果我们不希望驱动器被替换...
        ModItems.RIDER_BOOTS.get())
                
        .addRequiredItem( // 要求核心槽位有铁锭
                TEST_CORE_SLOT,
                Items.IRON_INGOT
                )
.addEffect(MobEffects.DAMAGE_BOOST, 0, 0, false);
```

### 💥 4. 吃瘪系统
生命值过低时强制解除变身 (我的身体已经菠萝菠萝哒！)

变身冷却机制

### 🎭 5. 事件系统
丰富的变身事件监听

物品插入/取出事件

形态切换事件

自定义动画支持

```java
// 监听变身事件
@SubscribeEvent
public void onHenshin(HenshinEvent.Post event) {
Player player = event.getPlayer();
player.level().playSound(null, player, ModSounds.HENSHIN_SOUND, // 播放变身音效
SoundSource.PLAYERS, 1.0F, 1.0F);
}
```
### 🌐 6. 网络同步
实时同步变身状态

腰带数据同步

客户端/服务端数据一致性

主要是方便整合包作者吧 :)

### 🚀 动态生成形态(不稳定)
可能的形态组合数量太多, 注册不过来怎么办? (就针对Build和OOO来的)

我们甚至有半自动化的生成!
```java
// 假设根本没手动注册任何形态, 仅注册了riderConfig+腰带槽位
private static void registerDynamicMappings() { // 一个动态映射注册方法
    DynamicArmorRegistry.registerItemArmor(BuildItems.RABBIT_BOTTLE, ModItems.RABBIT_ELEMENT.get()); // 为物品绑定对应盔甲
    DynamicEffectRegistry.registerItemEffects(BuildItems.RABBIT_BOTTLE, MobEffects.JUMP); // 为物品绑定对应效果

    DynamicArmorRegistry.registerItemArmor(BuildItems.TANK_BOTTLE, ModItems.TANK_ELEMENT.get()); 
    DynamicEffectRegistry.registerItemEffects(BuildItems.TANK_BOTTLE, MobEffects.DAMAGE_RESISTANCE);
    
    // 然后叠就完了
}

// 当玩家插入"兔子满装瓶"和"坦克满装瓶"时
// 自动生成RabbitTank形态！
// 可以与手动注册的形态同时工作, 不过手动注册了形态并且被匹配到后动态的效果/盔甲映射就不管用了哦
```
## 🎮 玩家体验
装备驱动器

插入道具到腰带槽位

触发变身（按键/使用道具）

切换形态（更换腰带物品）

解除变身（手动/吃瘪强制解除）

## ⚙️ 进阶功能
暂停变身流程
```java
// 在FormConfig中设置
anyForm.setShouldPause(true); // 这样在变身时候会进入缓冲窗口
// 有啥变身动画都往这塞

// 在动画完成后继续变身
DriverActionManager.INSTANCE.completeTransformation(player); // 反正调用这个方法就好了
```
形态覆盖事件
```java
// 强制锁定特定形态
@SubscribeEvent
public void onFormOverride(FormOverrideEvent event) { // 通过监听FormOverrideEvent, 在系统匹配形态结束时触发
if (
        // 这里写你的判断条件
) {
    // 强制覆盖形态 (也就是条件满足时怎么变都只能变你规定的形态)
    event.setOverrideForm(ANY_FORM); 
    // 就可以实现一些邪恶的操作了(bushi
    }
}
```
## 🧪 示例
我们提供了两个完整示例：

ExampleBasic：基础骑士变身系统

ExampleDynamicForm：动态形态生成系统

``` java
// 在模组初始化中调用
ExampleBasic.init();
ExampleDynamicForm.init();
// 就可以感受, 体验一下了
```
# 📦 安装与使用
添加RideBattleLib到你的build.gradle

创建你的骑士配置！

编译并享受变身吧！

🌟 为什么选择RideBattleLib？
✅ 专门为假面骑士玩法设计

✅ 完整的变身生命周期管理

✅ 动态形态生成黑科技

✅ 丰富的扩展事件

✅ 吃瘪系统原汁原味

✅ 持续更新维护

"变身！" - 现在就在你的模组中实现这句经典台词吧！

RideBattleLib © 2023 JPigeon
在MIT许可证下开源 - 自由地创造你的骑士世界！
注: 在DeepSeek帮助下写的README.md