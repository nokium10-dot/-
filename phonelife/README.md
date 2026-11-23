# PHONE LIFE 3D — 1.2.0

Android first-person smartphone workshop simulator. Package: `com.phonelife.sim`.

## Current gameplay
- First-person 3D view with touch camera control and a large 190 dp movement joystick.
- On-screen buttons with Russian labels for interaction, shop, inventory, quests and settings.
- Live center reticle and target hint.
- Shaded low-poly 3D scene with high-resolution procedural material atlases for the workshop and outdoor district.
- Workshop equipment: repair bench, monitor, keyboard, microscope, shelves, device display counter, tools and customer.
- Expanded outdoor block with storefronts, apartment facades, windows, trees, street lamps, benches, road markings and a delivery van.
- Buy used phones, AMOLED screens, batteries and parts; assemble and sell refurbished phones.
- Step-by-step three-stage assembly and repair tasks, customer sales, repair orders, ruble economy, reputation, days, upgrades and five story chapters.
- Scooter interaction, vendor and an outdoor trading route.
- Autosave, inventory, stats, settings, controls guide and reset confirmation.

## Controls
- Large left joystick: move forward/back/left/right.
- Drag on the right side of the 3D view: look around.
- Action: interact with the highlighted nearby object.
- Shop: buy devices, parts and repair contracts.
- Inventory: inspect stock and business stats.
- Quests: read story chapters and current progression.
- Settings: save, controls help or start a new game.

## Build
`gradle :phonelife:assembleDebug`

The Gradle build generates four 2048×2048 material atlases and packages them as actual textures used by the OpenGL ES 2.0 renderer. APK output:
`phonelife/build/outputs/apk/debug/phonelife-debug.apk`

The release workflow validates APK structure, package name and version before publishing.
