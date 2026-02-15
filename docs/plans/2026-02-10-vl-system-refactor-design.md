# Phase 1: VL System Refactor Design

## Goal

Decouple Grim's setback mechanism from the check system, making every check independently configurable for:
- Enable/disable
- Alert only (no setback)
- Setback with configurable VL threshold
- Full disable (no detection, no setback)

This preserves Grim's prediction engine accuracy while giving server admins control over how violations are handled.

---

## Current Architecture Analysis

### What Grim Already Has (partial VL system)

Grim is NOT entirely without a VL system. The existing infrastructure includes:

| Component | Location | Current Behavior |
|-----------|----------|-----------------|
| `Check.violations` | `Check.java:24` | Tracks violation count per check |
| `Check.decay` | `Check.java:25` | VL decay rate (from `@CheckData`) |
| `Check.setbackVL` | `Check.java:26` | VL threshold before setback triggers |
| `Check.setbackIfAboveSetbackVL()` | `Check.java:156-161` | Compares VL to threshold, calls setback |
| `PunishmentManager` | `PunishmentManager.java` | Handles commands at VL thresholds |
| `punishments/*.yml` | Config resources | Defines threshold-based commands |
| `@CheckData(setback=25)` | `CheckData.java:21` | Default setback VL annotation |
| `grim.nosetback.<check>` | `Check.java:74` | Per-check permission to skip setback |

### The Problem: Three Categories of Setback Calls

**Category A: VL-controlled setbacks (already go through the VL system)**
These call `flagWithSetback()` or `flagAndAlertWithSetback()` which checks `violations > setbackVL`:
- Most checks that use `Check.flagWithSetback()` — these already respect `setbackVL`

**Category B: Direct violation setbacks (bypass VL threshold)**
These call `executeViolationSetback()` directly, ignoring the VL system:

| File | Line | Check | Method Called |
|------|------|-------|--------------|
| `OffsetHandler.java` | 70 | Simulation | `executeViolationSetback()` — has its own internal advantage threshold |
| `CrashA.java` | 28 | CrashA | `executeViolationSetback()` — impossible coordinates |
| `CrashC.java` | 27 | CrashC | `executeViolationSetback()` — crash exploit |
| `KnockbackHandler.java` | 209 | Knockback | `executeViolationSetback()` — setback velocity resend |
| `PacketEntityReplication.java` | 399 | Firework abuse | `executeViolationSetback()` — scheduled task |
| `PacketPlayerAbilities.java` | 44 | Abilities | `executeViolationSetback()` — flight state mismatch |

| File | Line | Check | Method Called |
|------|------|-------|--------------|
| `Timer.java` | 83 | Timer | `executeNonSimulatingSetback()` |
| `TimerLimit.java` | 26 | TimerLimit | `executeNonSimulatingSetback()` |
| `SprintA.java` | 31 | SprintA | `executeNonSimulatingSetback()` |

**Category C: Force resyncs (structural, not violation-based)**
These are NOT punishment — they correct server/client desync:

| File | Line | Reason |
|------|------|--------|
| `MovementCheckRunner.java` | 78 | Unloaded chunk |
| `MovementCheckRunner.java` | 240 | Vehicle eject too far |
| `MovementCheckRunner.java` | 544 | Lag spike protection |
| `MovementCheckRunner.java` | 560 | Jump with setback velocity |
| `MovementCheckRunner.java` | 567 | Knockback ignore |
| `MovementCheckRunner.java` | 586 | Elytra exploit |
| `GhostBlockDetector.java` | 62 | Ghost block desync |
| `CheckManagerListener.java` | 707 | Block placement violation |
| `PacketEntityAction.java` | 54, 76 | Suspicious entity action |

---

## Design

### Principle: Don't Touch Category C

Force resyncs (Category C) are **structural corrections**, not punishments. They fix real server/client desync issues. These should remain hardcoded and untouched — disabling them would cause actual gameplay bugs, not just reduce punishment.

### Change 1: Unify Category B Into the VL System

Every Category B check should go through the existing `flagWithSetback()` / `flagAndAlertWithSetback()` flow instead of calling `executeViolationSetback()` or `executeNonSimulatingSetback()` directly.

**OffsetHandler.java** (most critical change):
```java
// BEFORE (line 67-71):
if ((advantageGained >= maxAdvantage || offset >= immediateSetbackThreshold)
        && !isNoSetbackPermission()
        && violations >= setbackViolationThreshold) {
    player.getSetbackTeleportUtil().executeViolationSetback();
}

// AFTER:
if ((advantageGained >= maxAdvantage || offset >= immediateSetbackThreshold)) {
    setbackIfAboveSetbackVL();  // Now respects setbackVL config + nosetback permission
}
```

Note: OffsetHandler already has its own `isNoSetbackPermission()` and `violations >= setbackViolationThreshold` — we replace these with the unified `setbackIfAboveSetbackVL()` which does the same check via `Check.shouldSetback()`.

**CrashA/CrashC**: These detect impossible coordinates and crash exploits. Setback is critical for safety, but we should still route through the VL system with a very low default `setbackvl` (e.g., 0 = instant setback on first flag).

**Timer/TimerLimit**: Currently call `executeNonSimulatingSetback()`. We need a variant: `setbackIfAboveSetbackVL()` that calls `executeNonSimulatingSetback()` instead of `executeViolationSetback()`. Add a new method:

```java
// In Check.java, new method:
public boolean nonSimulatingSetbackIfAboveSetbackVL() {
    if (shouldSetback()) {
        return player.getSetbackTeleportUtil().executeNonSimulatingSetback();
    }
    return false;
}
```

**SprintA**: Same treatment as Timer — use `nonSimulatingSetbackIfAboveSetbackVL()`.

**KnockbackHandler, PacketEntityReplication, PacketPlayerAbilities**: Route through `setbackIfAboveSetbackVL()`.

### Change 2: Per-Check Setback Mode in Config

Extend the existing config system to support a `setback-mode` per check:

```yaml
# In config/en.yml, per-check configuration:
Simulation:
  setbackvl: 25          # Already exists - VL threshold for setback
  decay: 0.02            # Already exists
  setback-mode: setback  # NEW: "setback" | "alert-only" | "disabled"

Timer:
  setbackvl: 10
  decay: 0.05
  setback-mode: setback

CrashA:
  setbackvl: 0           # Instant setback
  setback-mode: setback  # Crash protection should always setback
```

**`setback-mode` values:**
- `setback` (default): Current behavior. VL accumulates, setback when VL > setbackvl.
- `alert-only`: Check runs, VL accumulates, alerts fire, but NO setback ever. Useful for testing new checks or for checks with known false positive issues.
- `disabled`: Check is completely skipped. No detection, no VL, no alerts.

### Change 3: Implementation in Check.java

```java
// New enum in Check.java or a separate file:
public enum SetbackMode {
    SETBACK,      // Normal: setback when VL exceeds threshold
    ALERT_ONLY,   // Detect + alert, but never setback
    DISABLED       // Check completely disabled
}

// New field in Check.java:
private SetbackMode setbackMode = SetbackMode.SETBACK;

// Modified shouldSetback():
public boolean shouldSetback() {
    return setbackMode == SetbackMode.SETBACK
            && !noSetbackPermission
            && violations > setbackVL;
}

// Modified flag():
public final boolean flag(String verbose) {
    if (setbackMode == SetbackMode.DISABLED) return false;  // NEW
    if (player.disableGrim || (experimental && !player.isExperimentalChecks()) || exemptPermission)
        return false;
    // ... rest unchanged
}

// Modified reload():
@Override
public final void reload(ConfigManager configuration) {
    decay = configuration.getDoubleElse(configName + ".decay", decay);
    setbackVL = configuration.getDoubleElse(configName + ".setbackvl", setbackVL);
    // NEW: Load setback mode
    String modeStr = configuration.getStringElse(configName + ".setback-mode", "setback");
    setbackMode = switch (modeStr.toLowerCase()) {
        case "alert-only" -> SetbackMode.ALERT_ONLY;
        case "disabled" -> SetbackMode.DISABLED;
        default -> SetbackMode.SETBACK;
    };
    // ... rest unchanged
}
```

### Change 4: Update isEnabled Logic

Currently, `Check.isEnabled` is controlled by `PunishmentManager.reload()` — a check is only enabled if it appears in at least one punishment group. With the new system:

- If `setback-mode` is `disabled`, `isEnabled` should be `false` regardless of punishment config.
- If `setback-mode` is `setback` or `alert-only`, `isEnabled` follows existing punishment config logic.

This means `PunishmentManager.reload()` already handles enable/disable correctly — we just add an additional override:

```java
// In Check.java, add to reload():
if (setbackMode == SetbackMode.DISABLED) {
    isEnabled = false;
}
```

---

## Files to Modify

| File | Change | Risk |
|------|--------|------|
| `Check.java` | Add SetbackMode enum, field, modify `shouldSetback()`, `flag()`, `reload()`. Add `nonSimulatingSetbackIfAboveSetbackVL()` | LOW — additive changes |
| `CheckData.java` | No change needed (setback default already handled by config) | NONE |
| `OffsetHandler.java` | Replace direct `executeViolationSetback()` call with `setbackIfAboveSetbackVL()` | MEDIUM — core prediction check |
| `CrashA.java` | Replace direct call with `setbackIfAboveSetbackVL()` | LOW |
| `CrashC.java` | Replace direct call with `setbackIfAboveSetbackVL()` | LOW |
| `KnockbackHandler.java` | Replace direct call with `setbackIfAboveSetbackVL()` | MEDIUM — velocity system |
| `PacketEntityReplication.java` | Replace direct call with `setbackIfAboveSetbackVL()` | LOW |
| `PacketPlayerAbilities.java` | Replace direct call with `setbackIfAboveSetbackVL()` | LOW |
| `Timer.java` | Replace `executeNonSimulatingSetback()` with `nonSimulatingSetbackIfAboveSetbackVL()` | LOW |
| `TimerLimit.java` | Same as Timer | LOW |
| `SprintA.java` | Same as Timer | LOW |
| `config/en.yml` (+ other langs) | Add `setback-mode` documentation/defaults per check section | LOW |

**Files NOT modified:**
- `SetbackTeleportUtil.java` — No changes. The setback mechanism itself remains untouched.
- `MovementCheckRunner.java` — Force resyncs (Category C) remain untouched.
- `GhostBlockDetector.java` — Structural correction, not violation.
- `PunishmentManager.java` — Already works correctly with the VL system.
- `CheckManager.java` — No changes needed.

---

## Migration / Backwards Compatibility

- All default `setback-mode` values are `setback`, matching current behavior.
- Existing `setbackvl` and `decay` configs continue to work exactly as before.
- If a server admin has customized `setbackvl` values, they are preserved.
- The only new config key is `setback-mode`, which defaults to `setback` if absent.
- **Zero behavioral change with default config.**

---

## Testing Plan

1. Build and start a test server
2. Verify all existing checks still detect and setback with default config
3. Set `Simulation.setback-mode: alert-only` — verify movement cheats are detected and alerted but player is NOT setback
4. Set `Timer.setback-mode: disabled` — verify Timer check does not flag at all
5. Set `CrashA.setback-mode: alert-only` — verify crash packets are detected but not setback (this is unsafe, but should work as a config option)
6. Restore defaults — verify everything returns to normal behavior
7. Test with high-latency clients to confirm no new false positives introduced
