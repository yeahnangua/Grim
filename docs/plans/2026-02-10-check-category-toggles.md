# Check Category Toggles & In-Game GUI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add per-category check enable/disable + setback-mode controls in config.yml, a `disable-checks` list for granular sub-check disabling, and an in-game chest GUI for temporary runtime toggling.

**Architecture:** The category system sits between the existing per-check config and the global-setback-mode. A static mapping in `CheckManager` assigns each detection check to one of 13 categories. `BaseConfigManager` parses the new config sections. After `PunishmentManager.reload()` enables checks from punishments.yml, a new post-reload step applies category overrides and disable-checks. The Bukkit GUI creates chest inventories for runtime (memory-only) toggling.

**Tech Stack:** Java 21, Bukkit API (chest inventory GUI), incendo/cloud commands, PacketEvents, Gradle

---

## Task 1: Create CheckCategory Enum

**Files:**
- Create: `common/src/main/java/ac/grim/grimac/checks/CheckCategory.java`

**Step 1: Create the enum file**

```java
package ac.grim.grimac.checks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckCategory {
    MOVEMENT("movement", "Movement", "LIME_STAINED_GLASS_PANE"),
    ELYTRA("elytra", "Elytra", "ELYTRA"),
    COMBAT("combat", "Combat", "DIAMOND_SWORD"),
    VEHICLE("vehicle", "Vehicle", "MINECART"),
    TIMER("timer", "Timer", "CLOCK"),
    BADPACKETS("badpackets", "BadPackets", "BARRIER"),
    BREAKING("breaking", "Breaking", "IRON_PICKAXE"),
    PLACING("placing", "Placing", "BRICKS"),
    PACKET_ORDER("packet-order", "PacketOrder", "HOPPER"),
    MULTI_ACTIONS("multi-actions", "MultiActions", "REPEATER"),
    CRASH("crash", "Crash", "TNT"),
    SPRINT("sprint", "Sprint", "LEATHER_BOOTS"),
    CHAT("chat", "Chat", "WRITABLE_BOOK");

    private final String configKey;    // key in config.yml under "checks:"
    private final String displayName;  // shown in GUI
    private final String iconMaterial; // Bukkit Material name for GUI

    public static CheckCategory fromConfigKey(String key) {
        for (CheckCategory cat : values()) {
            if (cat.configKey.equalsIgnoreCase(key)) return cat;
        }
        return null;
    }
}
```

**Step 2: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :common:compileJava 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add common/src/main/java/ac/grim/grimac/checks/CheckCategory.java
git commit -m "feat: add CheckCategory enum for category-based check control"
```

---

## Task 2: Add Category Mapping to CheckManager

**Files:**
- Modify: `common/src/main/java/ac/grim/grimac/manager/CheckManager.java`

The mapping uses a static `Map<Class<? extends AbstractCheck>, CheckCategory>` to assign each detection check to a category. Internal infrastructure modules (PredictionRunner, SetbackTeleportUtil, CompensatedInventory, etc.) are intentionally NOT mapped and thus unaffected by category toggles.

**Step 1: Add the static category map**

Add these imports at the top of `CheckManager.java`:

```java
import ac.grim.grimac.checks.CheckCategory;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
```

Add the static field and a getter method after the `initedAtomic`/`inited` fields (after line 70):

```java
private static final Map<Class<? extends AbstractCheck>, CheckCategory> CATEGORY_MAP;

static {
    Map<Class<? extends AbstractCheck>, CheckCategory> map = new HashMap<>();

    // MOVEMENT
    map.put(OffsetHandler.class, CheckCategory.MOVEMENT);
    map.put(NoSlow.class, CheckCategory.MOVEMENT);
    map.put(Phase.class, CheckCategory.MOVEMENT);
    map.put(GroundSpoof.class, CheckCategory.MOVEMENT);
    map.put(NoFall.class, CheckCategory.MOVEMENT);

    // ELYTRA
    map.put(ElytraA.class, CheckCategory.ELYTRA);
    map.put(ElytraB.class, CheckCategory.ELYTRA);
    map.put(ElytraC.class, CheckCategory.ELYTRA);
    map.put(ElytraD.class, CheckCategory.ELYTRA);
    map.put(ElytraE.class, CheckCategory.ELYTRA);
    map.put(ElytraF.class, CheckCategory.ELYTRA);
    map.put(ElytraG.class, CheckCategory.ELYTRA);
    map.put(ElytraH.class, CheckCategory.ELYTRA);
    map.put(ElytraI.class, CheckCategory.ELYTRA);

    // COMBAT
    map.put(Reach.class, CheckCategory.COMBAT);
    map.put(Hitboxes.class, CheckCategory.COMBAT);
    map.put(MultiInteractA.class, CheckCategory.COMBAT);
    map.put(MultiInteractB.class, CheckCategory.COMBAT);
    map.put(AimModulo360.class, CheckCategory.COMBAT);
    map.put(AimDuplicateLook.class, CheckCategory.COMBAT);
    map.put(KnockbackHandler.class, CheckCategory.COMBAT);
    map.put(ExplosionHandler.class, CheckCategory.COMBAT);

    // VEHICLE
    map.put(VehicleA.class, CheckCategory.VEHICLE);
    map.put(VehicleB.class, CheckCategory.VEHICLE);
    map.put(VehicleC.class, CheckCategory.VEHICLE);
    map.put(VehicleD.class, CheckCategory.VEHICLE);
    map.put(VehicleE.class, CheckCategory.VEHICLE);
    map.put(VehicleF.class, CheckCategory.VEHICLE);
    map.put(VehicleTimer.class, CheckCategory.VEHICLE);

    // TIMER
    map.put(Timer.class, CheckCategory.TIMER);
    map.put(TimerLimit.class, CheckCategory.TIMER);
    map.put(NegativeTimer.class, CheckCategory.TIMER);

    // BADPACKETS
    map.put(BadPacketsA.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsB.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsC.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsD.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsE.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsF.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsG.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsH.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsI.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsJ.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsK.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsL.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsM.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsN.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsO.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsP.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsQ.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsR.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsS.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsT.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsU.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsV.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsW.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsX.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsY.class, CheckCategory.BADPACKETS);
    map.put(BadPacketsZ.class, CheckCategory.BADPACKETS);

    // BREAKING
    map.put(AirLiquidBreak.class, CheckCategory.BREAKING);
    map.put(WrongBreak.class, CheckCategory.BREAKING);
    map.put(RotationBreak.class, CheckCategory.BREAKING);
    map.put(FastBreak.class, CheckCategory.BREAKING);
    map.put(MultiBreak.class, CheckCategory.BREAKING);
    map.put(NoSwingBreak.class, CheckCategory.BREAKING);
    map.put(FarBreak.class, CheckCategory.BREAKING);
    map.put(InvalidBreak.class, CheckCategory.BREAKING);
    map.put(PositionBreakA.class, CheckCategory.BREAKING);
    map.put(PositionBreakB.class, CheckCategory.BREAKING);

    // PLACING
    map.put(InvalidPlaceA.class, CheckCategory.PLACING);
    map.put(InvalidPlaceB.class, CheckCategory.PLACING);
    map.put(AirLiquidPlace.class, CheckCategory.PLACING);
    map.put(MultiPlace.class, CheckCategory.PLACING);
    map.put(FarPlace.class, CheckCategory.PLACING);
    map.put(FabricatedPlace.class, CheckCategory.PLACING);
    map.put(PositionPlace.class, CheckCategory.PLACING);
    map.put(RotationPlace.class, CheckCategory.PLACING);
    map.put(DuplicateRotPlace.class, CheckCategory.PLACING);

    // PACKET_ORDER
    map.put(PacketOrderA.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderB.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderC.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderD.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderE.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderF.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderG.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderH.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderI.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderJ.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderK.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderL.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderM.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderN.class, CheckCategory.PACKET_ORDER);
    map.put(PacketOrderO.class, CheckCategory.PACKET_ORDER);

    // MULTI_ACTIONS
    map.put(MultiActionsA.class, CheckCategory.MULTI_ACTIONS);
    map.put(MultiActionsB.class, CheckCategory.MULTI_ACTIONS);
    map.put(MultiActionsC.class, CheckCategory.MULTI_ACTIONS);
    map.put(MultiActionsD.class, CheckCategory.MULTI_ACTIONS);
    map.put(MultiActionsE.class, CheckCategory.MULTI_ACTIONS);
    map.put(MultiActionsF.class, CheckCategory.MULTI_ACTIONS);
    map.put(MultiActionsG.class, CheckCategory.MULTI_ACTIONS);

    // CRASH
    map.put(CrashA.class, CheckCategory.CRASH);
    map.put(CrashB.class, CheckCategory.CRASH);
    map.put(CrashC.class, CheckCategory.CRASH);
    map.put(CrashD.class, CheckCategory.CRASH);
    map.put(CrashE.class, CheckCategory.CRASH);
    map.put(CrashF.class, CheckCategory.CRASH);
    map.put(CrashG.class, CheckCategory.CRASH);
    map.put(CrashH.class, CheckCategory.CRASH);
    map.put(CrashI.class, CheckCategory.CRASH);

    // SPRINT
    map.put(SprintA.class, CheckCategory.SPRINT);
    map.put(SprintB.class, CheckCategory.SPRINT);
    map.put(SprintC.class, CheckCategory.SPRINT);
    map.put(SprintD.class, CheckCategory.SPRINT);
    map.put(SprintE.class, CheckCategory.SPRINT);
    map.put(SprintF.class, CheckCategory.SPRINT);
    map.put(SprintG.class, CheckCategory.SPRINT);

    // CHAT
    map.put(ChatA.class, CheckCategory.CHAT);
    map.put(ChatB.class, CheckCategory.CHAT);
    map.put(ChatC.class, CheckCategory.CHAT);
    map.put(ChatD.class, CheckCategory.CHAT);

    CATEGORY_MAP = Collections.unmodifiableMap(map);
}

public static CheckCategory getCategory(Class<? extends AbstractCheck> checkClass) {
    return CATEGORY_MAP.get(checkClass);
}

public static Map<Class<? extends AbstractCheck>, CheckCategory> getCategoryMap() {
    return CATEGORY_MAP;
}

/**
 * Get all checks in a given category from this player's check manager.
 */
public List<AbstractCheck> getChecksByCategory(CheckCategory category) {
    List<AbstractCheck> result = new ArrayList<>();
    for (Map.Entry<Class<? extends AbstractCheck>, CheckCategory> entry : CATEGORY_MAP.entrySet()) {
        if (entry.getValue() == category) {
            AbstractCheck check = allChecks.get(entry.getKey());
            if (check != null) result.add(check);
        }
    }
    return result;
}
```

**Step 2: Add ExploitA/ExploitB import handling**

ExploitA and ExploitB don't fit neatly into the 13 categories. They're closest to CRASH (exploit prevention). Add them to CRASH in the static block:

```java
    // CRASH (includes exploit checks)
    map.put(ExploitA.class, CheckCategory.CRASH);
    map.put(ExploitB.class, CheckCategory.CRASH);
```

**Step 3: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :common:compileJava 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add common/src/main/java/ac/grim/grimac/manager/CheckManager.java
git commit -m "feat: add static check-to-category mapping in CheckManager"
```

---

## Task 3: Add Category Config Parsing to BaseConfigManager

**Files:**
- Modify: `common/src/main/java/ac/grim/grimac/manager/config/BaseConfigManager.java`

BaseConfigManager needs to parse the `checks:` section and `disable-checks:` list from config.yml. These are stored as global state (shared across all players).

**Step 1: Add category state fields and parsing**

Add these imports at the top:

```java
import ac.grim.grimac.checks.CheckCategory;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
```

Add fields after the `globalSetbackMode` field (after line 41):

```java
@Getter
private final Map<CheckCategory, Boolean> categoryEnabled = new EnumMap<>(CheckCategory.class);
@Getter
private final Map<CheckCategory, SetbackMode> categorySetbackMode = new EnumMap<>(CheckCategory.class);
@Getter
private final Set<String> disabledChecks = new HashSet<>();
```

Add parsing at the end of the `load()` method (before the closing brace on line 74):

```java
    // Parse category toggles
    categoryEnabled.clear();
    categorySetbackMode.clear();
    for (CheckCategory category : CheckCategory.values()) {
        String key = "checks." + category.getConfigKey();
        categoryEnabled.put(category,
                config.getBooleanElse(key + ".enabled", true));
        categorySetbackMode.put(category,
                SetbackMode.fromString(config.getStringElse(key + ".setback-mode", "setback")));
    }

    // Parse disable-checks list
    disabledChecks.clear();
    List<String> disabled = config.getStringListElse("disable-checks", new ArrayList<>());
    for (String name : disabled) {
        disabledChecks.add(name.toLowerCase(Locale.ROOT));
    }
```

Add `Locale` import: `import java.util.Locale;`

**Step 2: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :common:compileJava 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add common/src/main/java/ac/grim/grimac/manager/config/BaseConfigManager.java
git commit -m "feat: parse checks category toggles and disable-checks from config"
```

---

## Task 4: Apply Category Overrides After Punishment Reload

**Files:**
- Modify: `common/src/main/java/ac/grim/grimac/player/GrimPlayer.java`

The reload order in `GrimPlayer.reload()` is currently:
1. `check.reload()` for each check (reads per-check config like decay, setbackvl, setback-mode)
2. `punishmentManager.reload()` (disables ALL checks, then re-enables ones in punishments.yml)

We need to add a step 3: apply category overrides and disable-checks.

**Step 1: Add the category override logic**

In `GrimPlayer.java`, find the `reload(ConfigManager config)` method. After the line `punishmentManager.reload(config);` (currently the last meaningful line), add:

```java
    // Apply category overrides (after punishment manager has set enabled states)
    applyCategoryOverrides();
```

Then add a new method in GrimPlayer:

```java
private void applyCategoryOverrides() {
    var configManager = GrimAPI.INSTANCE.getConfigManager();
    var categoryEnabled = configManager.getCategoryEnabled();
    var categorySetbackMode = configManager.getCategorySetbackMode();
    var disabledChecks = configManager.getDisabledChecks();

    for (AbstractCheck check : checkManager.allChecks.values()) {
        CheckCategory category = CheckManager.getCategory(check.getClass());
        if (category == null) continue; // Internal module, skip

        // Category enabled override
        if (!categoryEnabled.getOrDefault(category, true)) {
            check.setEnabled(false);
            continue; // No need to set setback mode if disabled
        }

        // Category setback-mode override (only if check doesn't have its own per-check override)
        // Per-check setback-mode in config takes priority over category
        SetbackMode categoryMode = categorySetbackMode.getOrDefault(category, SetbackMode.SETBACK);
        if (check instanceof Check c) {
            String perCheckMode = configManager.getConfig().getStringElse(
                    c.getConfigName() + ".setback-mode", null);
            if (perCheckMode == null) {
                // No per-check override, use category mode
                // We need to set this via reflection or add a setter
                // For now, Check already reads setback-mode in reload()
                // We apply category mode only when per-check is not explicitly set
            }
        }

        // disable-checks list (highest priority)
        if (check.getCheckName() != null &&
                disabledChecks.contains(check.getCheckName().toLowerCase(Locale.ROOT))) {
            check.setEnabled(false);
        }
    }
}
```

Add imports at top of GrimPlayer.java:

```java
import ac.grim.grimac.checks.CheckCategory;
import ac.grim.grimac.checks.SetbackMode;
import java.util.Locale;
```

**Step 2: Add setback-mode setter to Check.java**

In `common/src/main/java/ac/grim/grimac/checks/Check.java`, the `setbackMode` field is private with no setter. Add a setter after the `setEnabled` setter annotation (line 35):

Change:
```java
private SetbackMode setbackMode = SetbackMode.SETBACK;
```
to include `@Setter` (lombok is already used for `isEnabled`):
```java
private @Setter SetbackMode setbackMode = SetbackMode.SETBACK;
```

**Step 3: Refine the category override logic with setback-mode support**

Update `applyCategoryOverrides()` in GrimPlayer to properly apply category setback-mode:

```java
private void applyCategoryOverrides() {
    var configManager = GrimAPI.INSTANCE.getConfigManager();
    var categoryEnabled = configManager.getCategoryEnabled();
    var categorySetbackMode = configManager.getCategorySetbackMode();
    var disabledChecks = configManager.getDisabledChecks();

    for (AbstractCheck check : checkManager.allChecks.values()) {
        CheckCategory category = CheckManager.getCategory(check.getClass());
        if (category == null) continue; // Internal module, skip

        // Category enabled override
        if (!categoryEnabled.getOrDefault(category, true)) {
            check.setEnabled(false);
            if (check instanceof Check c) c.setSetbackMode(SetbackMode.DISABLED);
            continue;
        }

        // Category setback-mode override
        if (check instanceof Check c) {
            SetbackMode categoryMode = categorySetbackMode.getOrDefault(category, SetbackMode.SETBACK);
            // Only apply category mode if the check didn't have a per-check override in config
            String perCheckMode = configManager.getConfig().getStringElse(
                    c.getConfigName() + ".setback-mode", null);
            if (perCheckMode == null && categoryMode != SetbackMode.SETBACK) {
                c.setSetbackMode(categoryMode);
                if (categoryMode == SetbackMode.DISABLED) {
                    c.setEnabled(false);
                }
            }
        }

        // disable-checks list (highest priority)
        if (check.getCheckName() != null &&
                disabledChecks.contains(check.getCheckName().toLowerCase(Locale.ROOT))) {
            check.setEnabled(false);
            if (check instanceof Check c) c.setSetbackMode(SetbackMode.DISABLED);
        }
    }
}
```

**Step 4: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :common:compileJava 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add common/src/main/java/ac/grim/grimac/player/GrimPlayer.java
git add common/src/main/java/ac/grim/grimac/checks/Check.java
git commit -m "feat: apply category overrides and disable-checks after punishment reload"
```

---

## Task 5: Update Config Templates

**Files:**
- Modify: `common/src/main/resources/config/en.yml`
- Modify: `common/src/main/resources/config/zh.yml`

Add the `checks:` section and `disable-checks:` list to each config template.

**Step 1: Add to en.yml**

Append before the `config-version:` line at the bottom:

```yaml
# ========== Check Category Controls ==========
# Control entire categories of checks. Each category has:
#   enabled: true/false - whether checks in this category run at all
#   setback-mode: "setback" / "alert-only" / "disabled"
# Priority: disable-checks > category setting > per-check setting > global-setback-mode
checks:
  movement:
    enabled: true
    setback-mode: setback
  elytra:
    enabled: true
    setback-mode: setback
  combat:
    enabled: true
    setback-mode: setback
  vehicle:
    enabled: true
    setback-mode: setback
  timer:
    enabled: true
    setback-mode: setback
  badpackets:
    enabled: true
    setback-mode: setback
  breaking:
    enabled: true
    setback-mode: setback
  placing:
    enabled: true
    setback-mode: setback
  packet-order:
    enabled: true
    setback-mode: setback
  multi-actions:
    enabled: true
    setback-mode: setback
  crash:
    enabled: true
    setback-mode: setback
  sprint:
    enabled: true
    setback-mode: setback
  chat:
    enabled: true
    setback-mode: setback

# Disable specific sub-checks by name (highest priority, overrides everything)
# Example: ["ElytraF", "BadPacketsE", "FastBreak"]
disable-checks: []
```

**Step 2: Add equivalent section to zh.yml**

Same structure but with Chinese comments:

```yaml
# ========== 检测分类控制 ==========
# 按分类控制检测项。每个分类有:
#   enabled: true/false - 是否启用该分类下的所有检测
#   setback-mode: "setback" / "alert-only" / "disabled"
# 优先级: disable-checks > 分类设置 > 单项设置 > global-setback-mode
checks:
  movement:
    enabled: true
    setback-mode: setback
  elytra:
    enabled: true
    setback-mode: setback
  combat:
    enabled: true
    setback-mode: setback
  vehicle:
    enabled: true
    setback-mode: setback
  timer:
    enabled: true
    setback-mode: setback
  badpackets:
    enabled: true
    setback-mode: setback
  breaking:
    enabled: true
    setback-mode: setback
  placing:
    enabled: true
    setback-mode: setback
  packet-order:
    enabled: true
    setback-mode: setback
  multi-actions:
    enabled: true
    setback-mode: setback
  crash:
    enabled: true
    setback-mode: setback
  sprint:
    enabled: true
    setback-mode: setback
  chat:
    enabled: true
    setback-mode: setback

# 按名称关闭特定子检测（最高优先级，覆盖一切）
# 示例: ["ElytraF", "BadPacketsE", "FastBreak"]
disable-checks: []
```

**Step 3: Update config-version**

Bump the `config-version:` value by 1 in both files (from `9` to `10`) so existing configs get the new defaults merged on upgrade.

**Step 4: Commit**

```bash
git add common/src/main/resources/config/en.yml common/src/main/resources/config/zh.yml
git commit -m "feat: add checks category toggles and disable-checks to config templates"
```

---

## Task 6: Create GrimChecks Command

**Files:**
- Create: `common/src/main/java/ac/grim/grimac/command/commands/GrimChecks.java`
- Modify: `common/src/main/java/ac/grim/grimac/command/CloudCommandService.java`

The command `/grim checks` opens the GUI for players, or prints category status to console.

**Step 1: Create the command class**

```java
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class GrimChecks implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("checks", Description.of("Open the check category toggle GUI"))
                        .permission("grim.checks")
                        .handler(this::handleChecks)
        );
    }

    private void handleChecks(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!sender.isPlayer()) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        // Fire an event or call platform-specific GUI opener
        // The GUI is Bukkit-specific, so we dispatch through GrimAPI
        GrimAPI.INSTANCE.getEventBus().post(
                new ac.grim.grimac.api.event.events.ChecksGuiOpenEvent(sender)
        );
    }
}
```

**Step 2: Create the ChecksGuiOpenEvent**

File: `common/src/main/java/ac/grim/grimac/api/event/events/ChecksGuiOpenEvent.java`

```java
package ac.grim.grimac.api.event.events;

import ac.grim.grimac.platform.api.sender.Sender;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChecksGuiOpenEvent {
    private final Sender sender;
}
```

**Step 3: Register the command in CloudCommandService**

In `common/src/main/java/ac/grim/grimac/command/CloudCommandService.java`, find the `registerCommands()` method. Add after the other command registrations (around line 49):

```java
new GrimChecks().register(commandManager, commandAdapter);
```

**Step 4: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :common:compileJava 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add common/src/main/java/ac/grim/grimac/command/commands/GrimChecks.java
git add common/src/main/java/ac/grim/grimac/api/event/events/ChecksGuiOpenEvent.java
git add common/src/main/java/ac/grim/grimac/command/CloudCommandService.java
git commit -m "feat: add /grim checks command with GUI open event"
```

---

## Task 7: Create Bukkit GUI - Main Category Panel

**Files:**
- Create: `bukkit/src/main/java/ac/grim/grimac/platform/bukkit/gui/ChecksCategoryGui.java`

This class creates a 54-slot chest GUI showing all 13 categories. Each category shows its current state (green/yellow/red). Left-click cycles through setback modes, right-click opens the sub-panel.

**Step 1: Create the main GUI class**

```java
package ac.grim.grimac.platform.bukkit.gui;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckCategory;
import ac.grim.grimac.checks.SetbackMode;
import ac.grim.grimac.manager.CheckManager;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChecksCategoryGui implements InventoryHolder {
    public static final String TITLE = ChatColor.DARK_GRAY + "Grim " + ChatColor.WHITE + "Check Categories";
    private final Inventory inventory;
    private final Player player;

    // Runtime overrides: category -> mode (null = use config default)
    private static final Map<CheckCategory, SetbackMode> runtimeCategoryModes =
            Collections.synchronizedMap(new EnumMap<>(CheckCategory.class));
    // Runtime disabled individual checks
    private static final Set<String> runtimeDisabledChecks =
            Collections.synchronizedSet(new HashSet<>());

    public ChecksCategoryGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, TITLE);
        populate();
    }

    private void populate() {
        inventory.clear();
        CheckCategory[] categories = CheckCategory.values();
        for (int i = 0; i < categories.length; i++) {
            CheckCategory cat = categories[i];
            SetbackMode mode = getEffectiveMode(cat);
            inventory.setItem(i, createCategoryItem(cat, mode));
        }
        // Close button at slot 53
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(closeMeta);
        inventory.setItem(53, close);
    }

    private ItemStack createCategoryItem(CheckCategory category, SetbackMode mode) {
        Material material;
        ChatColor color;
        switch (mode) {
            case SETBACK -> { material = Material.LIME_STAINED_GLASS_PANE; color = ChatColor.GREEN; }
            case ALERT_ONLY -> { material = Material.YELLOW_STAINED_GLASS_PANE; color = ChatColor.YELLOW; }
            case DISABLED -> { material = Material.RED_STAINED_GLASS_PANE; color = ChatColor.RED; }
            default -> { material = Material.LIME_STAINED_GLASS_PANE; color = ChatColor.GREEN; }
        }

        // Try to use the category's icon material, fall back to glass pane
        try {
            material = Material.valueOf(category.getIconMaterial());
        } catch (IllegalArgumentException ignored) {}

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + category.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Current mode: " + color + mode.name());

        // Show config vs runtime difference
        SetbackMode configMode = GrimAPI.INSTANCE.getConfigManager()
                .getCategorySetbackMode().getOrDefault(category, SetbackMode.SETBACK);
        boolean configEnabled = GrimAPI.INSTANCE.getConfigManager()
                .getCategoryEnabled().getOrDefault(category, true);
        if (!configEnabled) configMode = SetbackMode.DISABLED;

        if (runtimeCategoryModes.containsKey(category)) {
            lore.add(ChatColor.DARK_GRAY + "Config: " + configMode.name() + " (temporary override)");
        }

        // Count checks in this category
        int total = 0, disabled = 0;
        for (Map.Entry<Class<? extends AbstractCheck>, CheckCategory> entry :
                CheckManager.getCategoryMap().entrySet()) {
            if (entry.getValue() == category) {
                total++;
                String name = getCheckName(entry.getKey());
                if (name != null && runtimeDisabledChecks.contains(name.toLowerCase(Locale.ROOT))) {
                    disabled++;
                }
            }
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Checks: " + ChatColor.WHITE + total +
                (disabled > 0 ? ChatColor.RED + " (" + disabled + " disabled)" : ""));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Left-click: Cycle mode");
        lore.add(ChatColor.DARK_GRAY + "Right-click: View sub-checks");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(int slot, boolean isRightClick) {
        CheckCategory[] categories = CheckCategory.values();
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        if (slot < 0 || slot >= categories.length) return;

        CheckCategory category = categories[slot];

        if (isRightClick) {
            // Open sub-panel
            new ChecksDetailGui(player, category).open();
            return;
        }

        // Left-click: cycle mode
        SetbackMode current = getEffectiveMode(category);
        SetbackMode next = switch (current) {
            case SETBACK -> SetbackMode.ALERT_ONLY;
            case ALERT_ONLY -> SetbackMode.DISABLED;
            case DISABLED -> SetbackMode.SETBACK;
        };
        runtimeCategoryModes.put(category, next);
        applyRuntimeOverrides();
        populate();
    }

    public static SetbackMode getEffectiveMode(CheckCategory category) {
        if (runtimeCategoryModes.containsKey(category)) {
            return runtimeCategoryModes.get(category);
        }
        boolean enabled = GrimAPI.INSTANCE.getConfigManager()
                .getCategoryEnabled().getOrDefault(category, true);
        if (!enabled) return SetbackMode.DISABLED;
        return GrimAPI.INSTANCE.getConfigManager()
                .getCategorySetbackMode().getOrDefault(category, SetbackMode.SETBACK);
    }

    /**
     * Apply all runtime overrides to every online player's checks.
     * Called after any GUI change.
     */
    public static void applyRuntimeOverrides() {
        for (GrimPlayer grimPlayer : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            for (AbstractCheck check : grimPlayer.checkManager.allChecks.values()) {
                CheckCategory category = CheckManager.getCategory(check.getClass());
                if (category == null) continue;

                // Start from the punishment-manager-determined enabled state
                // We re-run the category logic on top
                if (runtimeCategoryModes.containsKey(category)) {
                    SetbackMode mode = runtimeCategoryModes.get(category);
                    if (mode == SetbackMode.DISABLED) {
                        check.setEnabled(false);
                        if (check instanceof Check c) c.setSetbackMode(SetbackMode.DISABLED);
                    } else {
                        // Re-enable (punishment manager may have enabled it)
                        if (check instanceof Check c) c.setSetbackMode(mode);
                    }
                }

                // Per-check runtime disable
                if (check.getCheckName() != null &&
                        runtimeDisabledChecks.contains(check.getCheckName().toLowerCase(Locale.ROOT))) {
                    check.setEnabled(false);
                    if (check instanceof Check c) c.setSetbackMode(SetbackMode.DISABLED);
                }
            }
        }
    }

    /**
     * Clear all runtime overrides (called on /grim reload).
     */
    public static void clearRuntimeOverrides() {
        runtimeCategoryModes.clear();
        runtimeDisabledChecks.clear();
    }

    public static Set<String> getRuntimeDisabledChecks() {
        return runtimeDisabledChecks;
    }

    private static String getCheckName(Class<? extends AbstractCheck> clazz) {
        var data = clazz.getAnnotation(ac.grim.grimac.checks.CheckData.class);
        if (data != null && !data.name().equals("UNKNOWN")) return data.name();
        return clazz.getSimpleName();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
```

**Step 2: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :bukkit:compileJava 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (may need adjustments based on actual Bukkit API availability)

**Step 3: Commit**

```bash
git add bukkit/src/main/java/ac/grim/grimac/platform/bukkit/gui/ChecksCategoryGui.java
git commit -m "feat: add main category GUI panel for check toggles"
```

---

## Task 8: Create Bukkit GUI - Sub-Check Detail Panel

**Files:**
- Create: `bukkit/src/main/java/ac/grim/grimac/platform/bukkit/gui/ChecksDetailGui.java`

Shows all individual checks within a category. Click to toggle individual checks on/off.

**Step 1: Create the detail GUI class**

```java
package ac.grim.grimac.platform.bukkit.gui;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckCategory;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.SetbackMode;
import ac.grim.grimac.manager.CheckManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChecksDetailGui implements InventoryHolder {
    private final Inventory inventory;
    private final Player player;
    private final CheckCategory category;
    private final List<Class<? extends AbstractCheck>> checkClasses;

    public ChecksDetailGui(Player player, CheckCategory category) {
        this.player = player;
        this.category = category;
        this.checkClasses = new ArrayList<>();

        // Collect all check classes in this category
        for (Map.Entry<Class<? extends AbstractCheck>, CheckCategory> entry :
                CheckManager.getCategoryMap().entrySet()) {
            if (entry.getValue() == category) {
                checkClasses.add(entry.getKey());
            }
        }
        // Sort by name
        checkClasses.sort(Comparator.comparing(c -> getCheckName(c)));

        int size = Math.min(54, ((checkClasses.size() / 9) + 2) * 9); // Round up to nearest 9, min 18
        size = Math.max(size, 18);
        String title = ChatColor.DARK_GRAY + "Grim " + ChatColor.WHITE + category.getDisplayName();
        this.inventory = Bukkit.createInventory(this, size, title);
        populate();
    }

    private void populate() {
        inventory.clear();
        Set<String> runtimeDisabled = ChecksCategoryGui.getRuntimeDisabledChecks();

        for (int i = 0; i < checkClasses.size() && i < inventory.getSize() - 9; i++) {
            Class<? extends AbstractCheck> clazz = checkClasses.get(i);
            String name = getCheckName(clazz);
            boolean disabled = runtimeDisabled.contains(name.toLowerCase(Locale.ROOT));

            ItemStack item = new ItemStack(disabled ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((disabled ? ChatColor.RED + "\u2717 " : ChatColor.GREEN + "\u2713 ") + name);

            List<String> lore = new ArrayList<>();
            lore.add(disabled
                    ? ChatColor.RED + "DISABLED (temporary)"
                    : ChatColor.GREEN + "ENABLED");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Click to toggle");

            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        // Back button in last row
        int backSlot = inventory.getSize() - 5;
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "Back");
        back.setItemMeta(backMeta);
        inventory.setItem(backSlot, back);
    }

    public void handleClick(int slot) {
        int backSlot = inventory.getSize() - 5;
        if (slot == backSlot) {
            new ChecksCategoryGui(player).open();
            return;
        }
        if (slot < 0 || slot >= checkClasses.size()) return;

        Class<? extends AbstractCheck> clazz = checkClasses.get(slot);
        String name = getCheckName(clazz);
        Set<String> runtimeDisabled = ChecksCategoryGui.getRuntimeDisabledChecks();

        String key = name.toLowerCase(Locale.ROOT);
        if (runtimeDisabled.contains(key)) {
            runtimeDisabled.remove(key);
        } else {
            runtimeDisabled.add(key);
        }

        ChecksCategoryGui.applyRuntimeOverrides();
        populate();
    }

    private static String getCheckName(Class<? extends AbstractCheck> clazz) {
        CheckData data = clazz.getAnnotation(CheckData.class);
        if (data != null && !data.name().equals("UNKNOWN")) return data.name();
        return clazz.getSimpleName();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
```

**Step 2: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :bukkit:compileJava 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add bukkit/src/main/java/ac/grim/grimac/platform/bukkit/gui/ChecksDetailGui.java
git commit -m "feat: add sub-check detail GUI panel"
```

---

## Task 9: Register GUI Event Listener in Bukkit

**Files:**
- Create: `bukkit/src/main/java/ac/grim/grimac/platform/bukkit/gui/ChecksGuiListener.java`
- Modify: `bukkit/src/main/java/ac/grim/grimac/platform/bukkit/GrimACBukkitLoaderPlugin.java` (register listener)

**Step 1: Create the Bukkit event listener**

```java
package ac.grim.grimac.platform.bukkit.gui;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.event.events.ChecksGuiOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ChecksGuiListener implements Listener {

    public ChecksGuiListener() {
        // Listen for the cross-platform GUI open event
        GrimAPI.INSTANCE.getEventBus().subscribe(ChecksGuiOpenEvent.class, event -> {
            Object nativeSender = event.getSender().getNativeSender();
            if (nativeSender instanceof Player bukkitPlayer) {
                // Must run on main thread for inventory operations
                GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(
                        GrimAPI.INSTANCE.getGrimPlugin(),
                        () -> new ChecksCategoryGui(bukkitPlayer).open()
                );
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof ChecksCategoryGui gui) {
            event.setCancelled(true);
            gui.handleClick(event.getRawSlot(), event.isRightClick());
        } else if (event.getInventory().getHolder() instanceof ChecksDetailGui gui) {
            event.setCancelled(true);
            gui.handleClick(event.getRawSlot());
        }
    }
}
```

**Step 2: Register the listener in the Bukkit plugin**

In `bukkit/src/main/java/ac/grim/grimac/platform/bukkit/GrimACBukkitLoaderPlugin.java`, find the `onEnable()` method. Add listener registration:

```java
@Override
public void onEnable() {
    GrimAPI.INSTANCE.start();
    // Register GUI listener
    getServer().getPluginManager().registerEvents(
            new ac.grim.grimac.platform.bukkit.gui.ChecksGuiListener(), this);
}
```

**Step 3: Clear runtime overrides on reload**

Find where `/grim reload` triggers. In `GrimPlayer.reload()`, the `applyCategoryOverrides()` method already handles config-based overrides. We also need to clear runtime GUI overrides when reload happens.

In `common/src/main/java/ac/grim/grimac/player/GrimPlayer.java`, at the start of the `reload(ConfigManager config)` method, before any other logic, check if this is the first player being reloaded (to avoid clearing per-player):

Better approach: clear runtime overrides in `BaseConfigManager.load()` since it runs once per reload. Add at the end of `BaseConfigManager.load()`:

```java
    // Note: runtime GUI overrides are cleared by the Bukkit GUI listener on reload
```

Actually, the simplest approach is to have `GrimAPI.INSTANCE.getExternalAPI().reloadAsync()` call `ChecksCategoryGui.clearRuntimeOverrides()`. But since ChecksCategoryGui is in the Bukkit module and the reload is in common, we should use the event bus.

Simpler: just clear in the first player's reload. Or: clear in BaseConfigManager.load() via a static callback.

The cleanest option: In `BaseConfigManager.load()`, set a flag `reloadGeneration` that increments. In `ChecksCategoryGui.applyRuntimeOverrides()`, check if generation changed and auto-clear. But this is over-engineering.

**Practical approach:** Add a reload hook interface. In `BaseConfigManager`, add:

```java
private Runnable onReloadHook;

public void setOnReloadHook(Runnable hook) {
    this.onReloadHook = hook;
}
```

At the end of `load()`:

```java
    if (onReloadHook != null) onReloadHook.run();
```

Then in `ChecksGuiListener` constructor:

```java
    GrimAPI.INSTANCE.getConfigManager().setOnReloadHook(ChecksCategoryGui::clearRuntimeOverrides);
```

**Step 4: Verify it compiles**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew :bukkit:compileJava 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add bukkit/src/main/java/ac/grim/grimac/platform/bukkit/gui/ChecksGuiListener.java
git add bukkit/src/main/java/ac/grim/grimac/platform/bukkit/GrimACBukkitLoaderPlugin.java
git add common/src/main/java/ac/grim/grimac/manager/config/BaseConfigManager.java
git commit -m "feat: register GUI event listener and reload hook in Bukkit"
```

---

## Task 10: Integration Testing

No automated tests exist in this project. All testing is done manually on a Minecraft test server.

**Step 1: Build the plugin**

Run: `cd /Users/txw/IdeaProjects/Grim && ./gradlew build 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 2: Deploy to test server**

Copy the built jar to the test server's plugins folder:
```bash
cp bukkit/build/libs/GrimAC-*.jar /Users/txw/server/plugins/
```

**Step 3: Manual test checklist**

Start the test server and verify each feature:

- [ ] Server starts without errors in console
- [ ] `config.yml` gains `checks:` section and `disable-checks:` on first boot with new config
- [ ] `/grim checks` opens the chest GUI
- [ ] All 13 categories show as green glass panes (all enabled by default)
- [ ] Left-clicking a category cycles: green (setback) -> yellow (alert-only) -> red (disabled) -> green
- [ ] Right-clicking a category opens sub-panel with individual checks listed
- [ ] Clicking a check in sub-panel toggles it red/green
- [ ] After toggling elytra category to disabled, elytra hacks are no longer detected
- [ ] `/grim reload` resets all GUI changes back to config defaults
- [ ] Setting `checks.elytra.enabled: false` in config.yml, then `/grim reload`, disables all elytra checks
- [ ] Adding `"ElytraF"` to `disable-checks:` list disables only ElytraF while other elytra checks remain active
- [ ] Setting `checks.combat.setback-mode: alert-only` makes combat checks only alert, no setback
- [ ] Per-check `setback-mode` in config (e.g., `Simulation.setback-mode: setback`) still overrides category mode
- [ ] Console shows appropriate messages, no errors or stack traces

**Step 4: Final commit**

If all tests pass and any fixes were needed, commit them:
```bash
git add -A
git commit -m "fix: integration test fixes for check category system"
```

---

## Architecture Diagram

```
Config Priority Chain (from lowest to highest):

  global-setback-mode          (baseline for all checks)
       ↑
  checks.{category}.enabled   (category-level toggle)
  checks.{category}.setback-mode
       ↑
  {CheckName}.setback-mode     (per-check config override)
       ↑
  disable-checks: [...]        (highest priority, disables specific checks)
       ↑
  GUI runtime toggles          (memory-only, reset on reload/restart)
```

```
Reload Sequence:

  GrimPlayer.reload(config)
    ├── check.reload() for each check     ← reads per-check config
    ├── punishmentManager.reload(config)  ← enables checks from punishments.yml
    └── applyCategoryOverrides()          ← NEW: applies categories + disable-checks
```

```
GUI Architecture:

  /grim checks
    → ChecksGuiOpenEvent (cross-platform event)
    → ChecksGuiListener (Bukkit) catches event
    → ChecksCategoryGui (54-slot chest, 13 category items)
        ├── Left-click: cycle setback mode (runtime only)
        └── Right-click: → ChecksDetailGui (lists sub-checks)
                              └── Click: toggle individual check (runtime only)
```
