# RideBattleLib - Transform into Kamen Riders in Minecraft!
"Cyclone! Joker! Now, count up your sins!"
Bring the iconic transformation scenes to your Minecraft world!

## 🎯 Overview
Welcome to RideBattleLib - a Kamen Rider transformation system library for modern Minecraft (1.21.1, NeoForge)! Imagine:
Inserting items into a driver, turning the driver, and shouting "Henshin!" - all within Minecraft!
Whether you want to recreate Build's Full Bottle system or create an ☆entirely new☆ Rider system, RideBattleLib makes it possible!

## ✨ Core Features
### 🧩 1. Rider Transformation System
Define driver items (belts)

Create transformation slot systems

Multiple activation methods: Key press, item use, auto-trigger

Complete state management (Idle → Transforming → Transformed)

```java
// Create a Rider Driver
RiderConfig myRider = new RiderConfig(MY_RIDER_ID)
    .setDriverItem(ModItems.ANY_DRIVER.get(), EquipmentSlot.LEGS) // Driver on legs(waist)
    .setAuxDriverItem(ModItems.ANY_AUX_DRIVER.get(), EquipmentSlot.OFFHAND); // Secondary driver in offhand
```
### ⚡ 2. Driver System
Item insertion/extraction mechanics

Slot validation system

Dual system: Main driver + Auxiliary driver

One-click item return (clear driver)

```java
RiderConfig myRider = new RiderConfig(MY_RIDER_ID)
    .addDriverSlot(
        SOME_SLOT,
        List.of(ModItems.ITEM_1.get(), ModItems.ITEM_2.get(), ModItems.ITEM_3.get()),
        true, // Required for transformation
        true) // Allow direct replacement
    .addAuxDriverSlot(/* similar parameters */);
```
### 🔮 3. Form System
Preconfigured forms (Base, Power-up, etc.)

Dynamic form generation (auto-create forms based on driver items!)

Form-specific attributes/effects

Form switching

```java
FormConfig anyForm = new FormConfig(ANY_FORM_ID)
    .setArmor(
        ModItems.RIDER_HELMET.get(),
        ModItems.RIDER_CHESTPLATE.get(),
        null, // Keep driver visible
        ModItems.RIDER_BOOTS.get())
    .addRequiredItem(TEST_CORE_SLOT, Items.IRON_INGOT)
    .addEffect(MobEffects.DAMAGE_BOOST, 0, 0, false);
```
### 💥 4. Penalty System
Force untransform at low HP ("My body is breaking down!")

Transformation cooldown mechanism

### 🎭 5. Event System
Rich transformation event listeners

Item insertion/extraction events

Form switching events

Custom animation support

```java
@SubscribeEvent
public void onHenshin(HenshinEvent.Post event) {
    Player player = event.getPlayer();
    player.level().playSound(null, player, ModSounds.HENSHIN_SOUND, 
        SoundSource.PLAYERS, 1.0F, 1.0F);
}
```
### 🌐 6. Network Synchronization
Real-time transformation state sync

Driver data synchronization

Client/server data consistency

Perfect for modpack creators!

### 🚀 Dynamic Form Generation (unstable)
Automatic form creation for complex systems like Build's bottles

Combine items to generate new forms dynamically

```java
// Dynamic armor mapping
DynamicArmorRegistry.registerItemArmor(BuildItems.RABBIT_BOTTLE, ModItems.RABBIT_ELEMENT.get());
DynamicEffectRegistry.registerItemEffects(BuildItems.RABBIT_BOTTLE, MobEffects.JUMP);

// Automatically creates RabbitTank form when inserted!
```
## 🎮 Player Experience
Equip a Rider Driver

Insert items into belt slots

Trigger transformation (key/item/auto)

Switch forms by changing driver items

Untransform (manual/penalty)

## ⚙️ Advanced Features
# Transformation Flow Control
```java
// Pause transformation for animations
anyForm.setShouldPause(true);

// Continue after animation
@SubscribeEvent
public static void onHenshin(HenshinEvent.Pre event) {
    // Your animation logic here
    DriverActionManager.INSTANCE.completeTransformation(player);
}
```
Form Override Event
```java
@SubscribeEvent
public void onFormOverride(FormOverrideEvent event) {
    if (/* conditions */) {
        event.setOverrideForm(ANY_FORM); // Force specific form
    }
}
```
# Skill System
Lightweight yet powerful skill interface:

```java
// 1. Create skill IDs
ResourceLocation FIRE_ATTACK = new ResourceLocation("yourmodid", "fire_attack");
ResourceLocation ICE_BREATH = new ResourceLocation("yourmodid", "ice_breath");

// 2. Register display names
SkillSystem.registerSkillName(FIRE_ATTACK, Component.translatable("skill.fire_attack"));
SkillSystem.registerSkillName(ICE_BREATH, Component.translatable("skill.ice_breath"));

// 3. Add to forms
FormConfig dragonForm = new FormConfig(DRAGON_FORM_ID)
    .addSkill(FIRE_ATTACK)
    .addSkill(ICE_BREATH);
```
Implement through event listeners:
```java
@SubscribeEvent
public static void onSkillTrigger(SkillEvent.Post event) {
    if (event.getSkillId().equals(FIRE_ATTACK)) {
        // Launch fireball
        player.level().addFreshEntity(new Fireball(...));
    } 
    else if (event.getSkillId().equals(ICE_BREATH)) {
        // Freeze area
    }
}
```
## 🧪 Examples
Two complete implementations included:

ExampleBasic: Basic transformation system

ExampleDynamicForm: Dynamic form generation

```java
// Initialize during mod setup
ExampleBasic.init();
ExampleDynamicForm.init();
```
# 📦 Installation & Usage
Add RideBattleLib to your build.gradle

Create your Rider configurations

Build and transform!

🌟 Why Choose RideBattleLib
✅ Designed specifically for Kamen Rider gameplay
✅ Complete transformation management
✅ Dynamic form generation technology
✅ Rich event system for extensibility
✅ Penalty system (for dramatic defeats!)
✅ Actively maintained and updated

"Henshin!" - Bring this iconic phrase to your mod today!

RideBattleLib © 2025 JPigeon
Open-sourced under MIT License - Create your Rider world freely!
PS: README translated with help of Deepseek
