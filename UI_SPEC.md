# Iron Fury: Last Stand — UI Spec

A 2D side-scrolling shooter for Android. The game canvas is drawn imperatively (Android `Canvas`), but for redesign purposes treat every screen below as a discrete "page." Orientation is **landscape**, target aspect ~16:9 (logical game size 800 × 450).

This doc lists every screen, every interactive element, every label, and every piece of data shown. Use it as the source of truth for the redesign — anything missing here doesn't exist in the game today.

---

## Global

- **Orientation:** landscape only.
- **Persistent data (SharedPreferences):** `coins` (int), `high_score` (int), `weapon_<NAME>` (bool unlock flag), `control_size`, custom button positions.
- **Color tone today:** dark navy/starfield backgrounds, red title, yellow accents, olive/khaki for enemies. Treat colors as open.
- **Typography today:** bold sans, all-caps for headings. Open to change.
- **Game canvas runs at logical 800 × 450** and is scaled to screen. UI elements should be designed at this aspect.

---

## Page 1 — Start Screen

Entry screen shown on app launch.

**Elements:**
- **Title:** `IRON FURY` (large, dominant, with drop shadow).
- **Subtitle:** `LAST STAND`.
- **Hero illustration:** stylized soldier character in the middle (animated subtly — breathing/idle).
- **Primary call-to-action:** `TAP TO START` text in the lower-middle. Whole screen is the tap target.
- **Stats row** (just below CTA): `HIGH SCORE: {n}` and `COINS: {n}`.
- **Tagline footer:** `20 LEVELS · 20 BOSSES · ENDLESS ACTION`.
- **Settings gear icon:** top-right corner. Tapping opens the Settings page.
- **Background:** dark gradient with star/particle field.

**Actions:**
- Tap anywhere (except gear) → starts the game (transitions to Gameplay HUD at level 1, or at last reached state if continuing — current behavior: always starts from level 1).
- Tap gear → Settings page.

---

## Page 2 — Settings

Modal-style full screen. Accessed from the Start Screen gear icon.

**Elements:**
- **Header:** `SETTINGS`.
- **Section: "Control Size"** — segmented picker with 4 options:
  - `SMALL` — "Smaller buttons · More screen space"
  - `MEDIUM` — "Default size · Balanced"
  - `LARGE` — "Bigger buttons · Easier to tap"
  - `EXTRA LARGE` — "Maximum size · Best for big fingers"
  - The currently selected option is highlighted; the description text below changes based on selection.
- **Preview area** — shows a mock D-Pad on the left and `JUMP` / `FIRE` round buttons on the right at the chosen size, so the user sees the size before applying. Label above: `Preview`.
- **Button: `CUSTOMIZE LAYOUT`** — enters drag-to-rearrange mode for the in-game controls (D-Pad / Jump / Fire). The user can drag each control to any position. Layout-edit mode has its own confirm/reset/cancel buttons (see Page 3).
- **Button: `BACK`** — returns to Start Screen.

**Persistent state:**
- `controlSize` enum (SMALL / MEDIUM / LARGE / EXTRA_LARGE).
- Custom (x, y) for D-Pad, Jump, Fire — null means use defaults.

---

## Page 3 — Control Layout Editor (sub-mode of Settings)

Triggered by `CUSTOMIZE LAYOUT`. Full-screen overlay over the game canvas.

**Elements:**
- The three controls (D-Pad, Jump button, Fire button) rendered draggable.
- Instructional text: e.g., "Drag controls to position them."
- **Save / Confirm** button.
- **Reset to Defaults** button.
- **Cancel** button.

**Actions:**
- Drag any control to reposition.
- Save → persists positions, returns to Settings.
- Reset → restores defaults (controls in bottom-left D-Pad, bottom-right Jump+Fire).
- Cancel → discards changes.

---

## Page 4 — Gameplay HUD (overlay during play)

The game canvas renders the world; the HUD is the overlay on top. This is the primary in-game screen.

**Top-left cluster:**
- **Lives indicator** — a row of heart icons. Current max is 3 (`PLAYER_MAX_LIVES`). Filled hearts = remaining lives. Empty hearts shown for lost lives up to max.
- **Level label** — below hearts: `LV.{n}  {ThemeName}` (e.g., `LV.3  Frozen Peaks`).

**Top-right cluster:**
- **Score:** `SCORE: {n}`.
- **Rocket ammo (conditional)**, shown only when the active weapon is the Rocket Launcher: `ROCKETS: {current}/{MAX_ROCKETS}`.

**Top-center:**
- **Active weapon label** — current weapon's display name, shown roughly at 14% from the top, center.

**Top-right floating element:**
- **Coins counter** — coin icon + numeric coin balance (persistent across runs). Rendered by the WeaponSelector overlay.

**Weapon selector (around top area, drawn by WeaponSelector):**
- Shows the row/cluster of unlocked weapons.
- Tap a weapon icon to equip it (cycles via prev/next or direct tap).
- Locked weapons show a price + lock icon; tapping with enough coins purchases & unlocks it.

**Bottom-left:**
- **D-Pad** — 4-directional (up, down, left, right). The "up" arrow on the D-Pad maps to aim-up while shooting; "down" maps to aim-down. Lateral inputs move the player.

**Bottom-right:**
- **JUMP** button (round).
- **FIRE** button (round). Holding fire = continuous shooting.

(All three control elements are draggable to custom positions via the layout editor.)

**Pause / Settings during gameplay:** today there is no in-game pause button — players exit via Android system back. Consider adding a pause icon in the redesign (top-right corner).

---

## Page 5 — Game Over

Triggered when the player loses their last life. Drawn as an overlay on top of the frozen game world.

**Elements:**
- **Headline:** `GAME OVER` (red, large).
- **Stat line:** `Level {n}  |  Score: {n}`.
- **CTA:** `Tap to Continue` — resumes at the last checkpoint (every 500 horizontal units) with lives refilled and current level/score preserved. (NOT a full restart.)

**Actions:**
- Tap anywhere → continue from last checkpoint.

---

## Page 6 — Level Complete

Triggered after defeating a level's boss when more levels remain.

**Elements:**
- **Headline (animated/pulsing):** `LEVEL {n} CLEAR!` (green).
- **Theme caption:** `"{ThemeName}" completed!` (gold).
- **Score:** `Score: {n}`.
- **Next-level teaser (if not final):** `Next: {NextThemeName}` (light blue).
- **CTA (pulsing alpha):** `Tap to Continue`.

**Actions:**
- Tap anywhere → advance to next level.

---

## Page 7 — Victory

Triggered after defeating the final boss (level 10).

**Elements:**
- **Headline:** `VICTORY!` (yellow, large).
- **Subtitle:** `All 20 Levels Conquered!`.
- **Stat:** `Final Score: {n}`.
- **CTA:** `Tap to Play Again` — full restart from level 1.

**Actions:**
- Tap anywhere → restart from level 1 (resets level/score, keeps coins & unlocked weapons).

---

## Page 8 — In-Game Weapon Selector (overlay component, not a standalone page)

Rendered as part of the HUD; relevant for redesign.

**Elements:**
- **Coin balance pill:** coin icon + amount.
- **Weapon icon row** — one button per weapon:
  - `MACHINE_GUN` (icon: M, default starter, always unlocked, free)
  - `SPREAD_GUN` (icon: S, price 150)
  - `LASER` (icon: L, price 300)
  - `ROCKET_LAUNCHER` (icon: R, price 500)
  - `FLAMETHROWER` (icon: F, price 400)
- **Per-weapon state:**
  - Unlocked + selected → highlighted/glowing.
  - Unlocked + not selected → normal.
  - Locked + affordable → shows price, tappable to purchase.
  - Locked + unaffordable → shows price, greyed out.

---

## Data / Reference

### Weapons (all values from `WeaponType.kt`)

| Weapon | Display | Fire rate (s) | Bullet speed | Damage | Bullets/shot | Spread | Explosive | Price |
|---|---|---|---|---|---|---|---|---|
| MACHINE_GUN | "Machine Gun" | 0.12 | 450 | 1 | 1 | 0° | no | 0 (free) |
| SPREAD_GUN | "Spread Gun" | 0.25 | 400 | 1 | 5 | 15° | no | 150 |
| LASER | "Laser" | 0.05 | 800 | 1 | 1 | 0° | no | 300 |
| ROCKET_LAUNCHER | "Rocket" | 0.80 | 250 | 3 | 1 | 0° | yes (r=60) | 500 |
| FLAMETHROWER | "Flame" | 0.03 | 300 | 1 | 1 | 8° | no | 400 |

### Levels & Themes

20 levels total, each with a unique theme:

1. Forest Dawn
2. Desert Storm
3. Frozen Peaks
4. Volcanic Depths
5. Neon City
6. Sunken Ruins
7. Sky Fortress
8. Haunted Swamp
9. Crystal Caverns
10. Final Assault
11. Toxic Wasteland
12. Abyssal Trench
13. Molten Core
14. Aurora Tundra
15. Orbital Station
16. Blood Marsh
17. Golden Dunes
18. Void Nebula
19. Ashen Battlefield
20. Dragon's Throne

Levels 11-20 are procedurally generated from a per-level seed (deterministic, so a level's layout is identical on every retry).

Each ends with a boss fight. Boss spawns when the player reaches 600 units before the end of the level.

### Player

- Max lives: 3 (refilled on Game Over → Continue, or on level advance).
- Checkpoint interval: every 500 horizontal units traveled (auto-saved during play).
- On non-fatal death: respawn at last checkpoint.
- On fatal death (Game Over → Continue): respawn at last checkpoint with full lives.

### Pickups

- **Coins** — dropped by enemies (1–3 per kill, value 10 or 25) and by bosses (more drops). Persistent currency.
- **PowerUps** — drop from enemies/bosses; grant weapons. Pickup type is a `WeaponType` reference.

### Enemy

Single archetype today: standing soldier that patrols left-right within fixed bounds, aims at player with lead prediction, fires at ~0.9s intervals.

---

## State Machine (page transitions)

```
START_SCREEN ──tap──▶ PLAYING
START_SCREEN ──gear──▶ SETTINGS
SETTINGS ──BACK──▶ START_SCREEN
SETTINGS ──CUSTOMIZE──▶ LAYOUT_EDIT ──save/cancel──▶ SETTINGS

PLAYING ──die last life──▶ GAME_OVER ──tap──▶ PLAYING (at checkpoint)
PLAYING ──boss defeated, more levels──▶ LEVEL_COMPLETE ──tap──▶ PLAYING (next level)
PLAYING ──final boss defeated──▶ VICTORY ──tap──▶ PLAYING (level 1, full restart)
```

---

## Notes for Redesign

- **Aspect:** design every screen at 16:9 landscape. Reserve safe areas for the notch on tall devices.
- **Touch targets:** D-Pad/Jump/Fire are draggable by the user — design them as floating, not docked to fixed positions. Default positions: D-Pad bottom-left, Jump+Fire bottom-right.
- **Persistent vs. session:** coins, high score, weapon unlocks, control size, layout positions persist across runs. Score, current level, lives, current weapon are session-only.
- **No login / no online / no ads / no IAP** in the current build. The economy is closed (coins from gameplay only).
- **No pause screen, no level select, no achievements, no profile, no leaderboard** today — flag any of these as additions if introducing them, don't assume they exist.
- **What's rendered in code today vs. what would be assets:** all UI is hand-drawn on `Canvas` (text, shapes, gradients). A redesign that introduces image/SVG assets needs corresponding asset files.
