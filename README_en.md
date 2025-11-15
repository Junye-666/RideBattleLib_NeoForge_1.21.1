# RideBattleLib - Transform into Kamen Rider in MC!
[简体中文](README.md) | **English** 

"Cyclone! Joker! Now, count up your sins!"

Now you can shout your transformation catchphrase in Minecraft!

## 🎯 Overview
Welcome to RideBattleLib - a Kamen Rider transformation system library built for modern Minecraft versions (1.21.1/1.21.8, NeoForge)!

Insert items, activate the driver, and shout "Henshin!" in Minecraft!

Whether you want to recreate Build's Full Bottle system or create a ☆Brand New☆ Rider System, RideBattleLib makes it easy to achieve!

This page is for 1.21.1, [1.21.8 is here](https://github.com/Junye-666/RideBattleLib_NeoForge_1.21.8)

## ✨ Core Features
### 🧩 1. Rider Transformation System
- **Complete transformation state management** - Idle → Transforming → Transformed

```java
// Create rider configuration
public static final ResourceLocation MY_RIDER_ID =
        ResourceLocation.fromNamespaceAndPath(MODID, "kamen_rider_demo");

public static final RiderConfig MY_RIDER = new RiderConfig(MY_RIDER_ID)
        .setMainDriverItem(ModItems.DRIVER_ITEM.get(), EquipmentSlot.LEGS)
        .setAuxDriverItem(ModItems.AUX_DRIVER.get(), EquipmentSlot.OFFHAND)
        .setTriggerItem(ModItems.TRIGGER_ITEM.get())
        .setAllowDynamicForms(true);
```

### ⚡ 2. Driver System
- **Dual Driver System** - Main Driver + Auxiliary Driver

```java
// Define items accepted by the driver
RiderConfig MY_RIDER = new RiderConfig(MY_RIDER_ID)
                // Add main driver slot
                .addMainDriverSlot(
                        ResourceLocation.fromNamespaceAndPath(MODID, "core_slot"),
                        List.of(
                                ModItems.CORE_ITEM_1.get(),
                                ModItems.CORE_ITEM_2.get(),
                                ModItems.CORE_ITEM_3.get()
                        ),
                        true,  // Whether required
                        true   // Whether content can be directly replaced
                )
                // Add auxiliary driver slot  
                .addAuxDriverSlot(
                        ResourceLocation.fromNamespaceAndPath(MODID, "aux_slot"),
                        List.of(ModItems.AUX_ITEM.get()),
                        false,
                        false
                );
```
### 🔮 3. Form System
- **Predefined Forms** + **Dynamic Form Generation**

```java
// Create predefined form
FormConfig baseForm = new FormConfig(
                ResourceLocation.fromNamespaceAndPath(MODID, "base_form")
        )
                .setArmor(
                        ModItems.RIDER_HELMET.get(),
                        ModItems.RIDER_CHESTPLATE.get(),
                        null,  // Keep driver from being replaced
                        ModItems.RIDER_BOOTS.get()
                )
                .addRequiredItem(
                        ResourceLocation.fromNamespaceAndPath(MODID, "core_slot"),
                        ModItems.CORE_ITEM_1.get()
                )
                .addEffect(MobEffects.DAMAGE_BOOST, 0)
                .addSkill(ResourceLocation.fromNamespaceAndPath(MODID, "rider_kick"));

// Register to rider
MY_RIDER.addForm(baseForm);
```
### 🚀 4. Dynamic Form System
Too many possible form combinations to register manually? (Looking at you, Build and OOO!)

Automatically generate forms based on driver items!
```java
// Register dynamic form mappings
private static void registerDynamicMappings() {
    // Item to armor mapping
    DynamicFormConfig.registerItemArmor(
        ModItems.RABBIT_BOTTLE.get(), 
        EquipmentSlot.HEAD, 
        ModItems.RABBIT_ARMOR.get()
    );
    
    // Item to effect mapping
    DynamicFormConfig.registerItemEffect(
        ModItems.RABBIT_BOTTLE.get(),
        MobEffects.JUMP,
        114514,  // Duration
        1,       // Level
        false    // Hide particles
    );
    
    // Register undersuit configuration
    DynamicFormConfig.registerRiderUndersuit(
        MY_RIDER_ID,
        ModItems.UNDERSUIT_HELMET.get(),
        ModItems.UNDERSUIT_CHESTPLATE.get(),
        ModItems.UNDERSUIT_LEGGINGS.get(), 
        ModItems.UNDERSUIT_BOOTS.get()
    );
}
```
### 💥 5. Penalty System
Forcefully cancel transformation when health is too low + Cooldown mechanism
🎭 6. Event System
Complete transformation lifecycle events
```java
// Transformation events
@SubscribeEvent
public static void onHenshin(HenshinEvent.Pre event) {
    // Pre-transformation logic (animations, sound effects, etc.)
    Player player = event.getPlayer();
    player.level().playSound(null, player, 
        SoundEvents.LIGHTNING_BOLT_THUNDER, 
        SoundSource.PLAYERS, 1.0F, 1.0F
    );
}

@SubscribeEvent  
public static void onHenshin(HenshinEvent.Post event) {
    // Post-transformation logic
    RiderManager.playPublicSound(event.getPlayer(), SoundEvents.PLAYER_LEVELUP);
}

// Form switch events
@SubscribeEvent
public static void onFormSwitch(FormSwitchEvent.Post event) {
    Player player = event.getPlayer();
    player.displayClientMessage(
        Component.literal("Form Switch: " + event.getNewFormId()),
        true
    );
}
```
### ⚡ 7. Skill System
Multiple skill rotation support
```java
// Register skills
SkillSystem.registerSkillName(
    ResourceLocation.fromNamespaceAndPath(MODID, "rider_kick"), // Skill ID for identification
    Component.translatable("skill.mymod.rider_kick") // Multi-language support
);

// Listen for skill triggers
@SubscribeEvent  
public static void onSkillTrigger(SkillEvent.Post event) {
    if (event.getSkillId().getPath().equals("rider_kick")) {
        Player player = event.getPlayer();
        // Execute rider kick logic
        performRiderKick(player);
    }
}

private static void performRiderKick(Player player) {
    // Implement rider kick skill
    Vec3 lookVec = player.getLookAngle();
    player.setDeltaMovement(lookVec.x * 2, 1.0, lookVec.z * 2);
    player.hurtMarked = true;
    
    // Damage surrounding enemies
    AABB area = player.getBoundingBox().inflate(3);
    // ... Damage logic
}
```
### 🌐 8. Network Synchronization
Complete client-server synchronization
```java
// Manual state synchronization (if needed)
if (player instanceof ServerPlayer serverPlayer) {
        RiderManager.syncClientState(serverPlayer);
    RiderManager.syncDriverData(serverPlayer); 
    RiderManager.syncHenshinState(serverPlayer);
}
```
## 🎮 Player Guide
### Basic Operation Flow:

Equip Driver - Equip driver item to specified slot

Insert Items - Right-click items to insert into driver slots

Trigger Transformation - Press G key or use trigger item

Switch Forms - Change items in the driver

Use Skills - Press R to trigger current skill, Shift + R to switch skills

Cancel Transformation - Press V to manually cancel, or automatically when health is too low (Penalty)

### Default Key Bindings:

**G** - Activate driver

**V** - Cancel transformation

**R** - Use skill / Shift + R switch skills

**X** - Return all driver items

## ⚙️ Advanced Features
### Transformation Flow Control
```java
FormConfig advancedForm = new FormConfig(FORM_ID)
        .setShouldPause(true)  // Enable transformation pause (wait for animation completion)
        .setTriggerType(TriggerType.ITEM);  // Trigger type: KEY/ITEM/AUTO
```
Subscribing to HenshinEvent/FormSwitchEvent:
```java
@SubscribeEvent
public static void onHenshin(HenshinEvent.Pre event) {
    // Transformation animations, effects, sound, special logic
}

@SubscribeEvent
public static void onFormSwitch(FormSwitchEvent.Pre event) {
    // Form switch animations, effects, sound, special logic
}

// Continue transformation after animation completion
    // Call method
    RiderManager.completeHenshin(player); 
```
### Form Override
```java
@SubscribeEvent
public static void onFormOverride(FormOverrideEvent event) {
    // Force lock specific form
    if (event.getPlayer().getHealth() < 10.0f) {
        event.setOverrideForm(ResourceLocation.fromNamespaceAndPath(MODID, "emergency_form"));
    }
}
```
### Quick API Usage
```java
// Use RiderManager quick methods
RiderManager.transform(player);                    // Attempt transformation
RiderManager.unTransform(player);                  // Cancel transformation
RiderManager.switchForm(player, newFormId);        // Switch forms
RiderManager.isTransformed(player);                // Check transformation status
RiderManager.getDriverItems(player);               // Get driver items
RiderManager.penaltyUntransform(player);           // Force penalty cancellation
```
## 🧪 Examples
```java
// Call during mod initialization
ExampleBasic.init();        // Basic rider system
ExampleDynamicForm.init();  // Dynamic form system
```
# 📦 Installation & Usage
## 🌟 Why Choose RideBattleLib?

✅ Specifically designed for Kamen Rider gameplay

✅ Complete transformation management

✅ Dynamic form generation black magic (just kidding)

✅ Rich extension events

✅ Penalty system (lol)

✅ Continuous updates and maintenance

### For Mod Developers:
- 🚀 Quick start - Create your first rider in 10 minutes
- 🎨 Highly customizable - Everything from drivers to skills can be configured
- 🔧 Stable and reliable - Complete error handling and network synchronization

### For Players:
- 🎮 Immersive experience - Complete transformation flow and effects
- ⚡ Smooth operation - Optimized performance and responsiveness 

### 🐛 Troubleshooting
Common Issues:
- Transformation not working - Check driver slot configuration and required items
- Form not matching - Verify item mappings and slot definitions
- Network out of sync - Ensure proper synchronization method calls
- Enable Debug Mode: Turn on Debug Mode in the Config page

"Henshin!" - Now implement this classic catchphrase in your mod!

RideBattleLib © 2025 JPigeon
Open source under MIT License - Freely create your Rider world!

PS: README translated with help of Deepseek
