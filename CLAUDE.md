# Habits – Android habit tracker

Offline Android app (Kotlin, Jetpack Compose, Material 3) to build and quit habits. UI language: **German**.
Single user, no server. See README.md for the feature/concept overview.

## Layout
- `core/` – pure Kotlin/JVM domain logic, **no Android imports**. Everything that decides success, streaks, rates,
  challenges, XP and achievements lives here and is unit-tested.
  - `Habit.kt` – model: `Direction` (BUILD/QUIT), `Measure` (CHECK/COUNT), `Schedule` (OnDays / PerWeek),
    versioned `Plan`s, `Pause`s, `Challenge`
  - `Evaluation.kt` – `HabitHistory`: periods (a due day or a Mon–Sun week), streaks ("never miss twice"),
    rates, challenge status, consolidation (66 days / 80 %)
  - `Gamification.kt` – `Overview` across habits, XP, levels, achievements
- `app/` – Android: `data/` (SQLite via `SQLiteOpenHelper`, habits stored as JSON, `Repository` with a
  `StateFlow<AppState>`), `reminder/` (AlarmManager + notifications), `ui/` (Compose screens), `MainActivity`
  (tabs + simple state-based navigation, no navigation library)
- `app/src/debug/` – Compose previews + `SampleData` (debug-only), dev app name "Habits (Dev)"

## Commands
```bash
./gradlew :core:test                    # domain tests (fast, no device needed) – run after any core change
./gradlew :app:assembleDebug            # compile the app
./gradlew :app:installDebug             # install dev build on the connected device/emulator
adb shell am start -n de.habits.app.debug/de.habits.app.MainActivity   # launch it
adb exec-out screencap -p > /tmp/habits.png                              # screenshot to inspect the UI
adb logcat -s AndroidRuntime:E          # crashes
```
The debug build has applicationId `de.habits.app.debug`, so it installs next to the release app from CI
(`de.habits.app`, signed with the key from GitHub secrets) and has its own data. Never commit keystores.

## Conventions
- Put logic in `core` with a test in `core/src/test/.../CoreTest.kt`; UI code only renders and forwards events.
- Plan changes never rewrite history: use `Habit.withPlan(date, …)` / `withStartDate`, don't edit `plans` directly.
- Day-level "success" is always `Habit.isOk(amount, plan)`; period outcomes come from `HabitHistory.periods`.
- New/changed screens: add or update a preview in `app/src/debug/.../preview/Previews.kt`
  (screens take plain state + callbacks, so previews only need `SampleData`). Code that needs system services
  must be skipped under `LocalInspectionMode`.
- Keep the UI calm: lists over cards, few emojis, colours from `ui/Theme.kt` (`Status`, `habitColors`).
- Versions are pinned in the build files (AGP 8.7, Kotlin 2.1, Compose BOM 2024.12); bump deliberately.

## CI
`.github/workflows/build.yml` runs `:core:test :app:assembleDebug :app:assembleRelease` (debug compiles the previews) on every push and uploads the APK as
artifact `habits-apk`.
