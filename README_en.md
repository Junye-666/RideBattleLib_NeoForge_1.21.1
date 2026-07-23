# RideBattleLib – Become a Kamen Rider in Minecraft!

[简体中文](README.md) | **English**

> "Cyclone! Joker! Now, count up your sins!"

RideBattleLib is a **Kamen Rider transformation system API** built for **Minecraft NeoForge 1.21.1 / 1.21.11 / 26.1+**.  
It provides a complete framework for drivers, forms, skills, events, and network synchronization, allowing developers to quickly build their own Rider mods.

---

## 🎯 Key Features

- **One‑stop Rider Builder** – Use `RiderBuilder` + `FormBuilder` to define Riders and forms with ease.
- **Dual Driver System** – Main driver + auxiliary driver, with customisable slots and item rules.
- **Dynamic Form Generation** – Automatically combine armour, effects, and granted items based on items placed in the driver; register mappings with `DynamicMappingBuilder`.
- **Event‑Driven Lifecycle** – Everything from transformation, form switching, skill triggers, to penalty (defeat) can be intercepted via events.
- **Full Network Synchronisation** – Server ↔ client state sync is handled automatically – no extra work required.
- **Built‑in Skill Rotation** – Supports multiple skills with cooldowns, switching, and triggering.
- **Penalty System** – Forces untransformation when health is too low and applies a cooldown; thresholds and penalties are configurable.

---

## 🚀 Quick Start

### 1. Create Your First Rider (using the Builder)

```java
RiderConfig rider = RiderBuilder.create(
        ResourceLocation.fromNamespaceAndPath("mymod", "kamen_rider_zero_one")
    )
    .driver(ModItems.ZERO_ONE_DRIVER.get(), EquipmentSlot.LEGS)
    .auxDriver(ModItems.AUX_DRIVER.get(), EquipmentSlot.OFFHAND)
    .slot(
        ResourceLocation.fromNamespaceAndPath("mymod", "core_slot"),
        List.of(Items.IRON_INGOT, Items.GOLD_INGOT),
        true,  // required
        true   // allow replacement
    )
    .form(ResourceLocation.fromNamespaceAndPath("mymod", "rising_hopper"))
        .armor(
            ModItems.RISING_HELMET.get(),
            ModItems.RISING_CHEST.get(),
            null,   // keep the driver slot free
            ModItems.RISING_BOOTS.get()
        )
        .requiredItem(
            ResourceLocation.fromNamespaceAndPath("mymod", "core_slot"),
            Items.IRON_INGOT
        )
        .effect(MobEffects.JUMP, 1)
        .skill("rider_kick", Component.literal("Rider Kick"), 30)
        .end()
    .baseForm("rising_hopper")
    .allowDynamicForms(true)
    .buildAndRegister();  // auto‑register
```
### 2. Dynamic Form Mappings (using DynamicMappingBuilder)
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
    .register();  // register all mappings
```
### 3. Use RideBattleAPI for Quick Control
```java
// In any event or method
Player player = ...;
RideBattleAPI.transform(player);          // attempt transformation
RideBattleAPI.unTransform(player);        // untransform
RideBattleAPI.switchForm(player, newFormId);
RideBattleAPI.triggerCurrentSkill(player);
boolean isTransformed = RideBattleAPI.isTransformed(player);
Map<ResourceLocation, ItemStack> items = RideBattleAPI.getDriverItems(player);
```
## 📚 Full Documentation & Examples
Advanced Tutorials / Complete API Usage → [GitHub Wiki](https://github.com/Junye-666/RideBattleLib_NeoForge_1.21.1/wiki) (covers event listening, custom strategies, network sync, and more)

Example classes: ExampleBasic and ExampleDynamicForm demonstrate basic usage and dynamic forms.

## ⚙️ Configuration
The mod provides a common config file (ridebattlelib-common.toml) where you can adjust:

Penalty trigger threshold, cooldown duration, explosion power, knockback strength

Key press debounce delay

Debug mode switches

## 🤝 Contributing & License
Open‑source under the MIT License

Issues and PRs are welcome on the GitHub repository

Start building your own Rider world now!

"Henshin!"

PS: README translated with help of Deepseek