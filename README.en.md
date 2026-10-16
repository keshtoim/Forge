<div align="center">

# Forge

### A strength training log without the clutter

[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![CI](https://img.shields.io/github/actions/workflow/status/keshtoim/Forge/android.yml?style=for-the-badge&label=CI)](https://github.com/keshtoim/Forge/actions)
[![Release](https://img.shields.io/github/v/release/keshtoim/Forge?style=for-the-badge)](https://github.com/keshtoim/Forge/releases)

**[🇷🇺 Русский](README.md) · [🇬🇧 English](README.en.md)**

</div>

---

## Why another workout log?

Most fitness apps want an account, show ads and lock stats behind a subscription. Forge does one thing: **log a set fast and see your progress**. No sign-up, no ads, no cloud.

- 📴 **100% offline**: the app doesn't even have the internet permission
- 🏋️ **Built for the gym**: log a set in a couple of taps, last session's numbers are filled in for you
- 🧍 **Muscle map**: see what an exercise works and what you've trained this month
- 🔓 **Open source**: no trackers or analytics

---

## Features

<table>
<tr>
<td width="50%">

### 🏋️ Workout
- Workout templates with an editor
- Focus mode on one exercise at a time
- **Ghost values**: last session's weight and reps in every set
- Sets listed top to bottom, "+ Set" always within reach
- **🎨 Set level**: warm-up, light, medium, heavy, max, each with its own color and hint
- Warm-up sets don't count toward records and stats
- **⚖️ Smart weight**: plates per side plus the bar
- ⏱️ Rest timer in a notification (foreground service)
- An active workout survives closing the app

</td>
<td width="50%">

### 📚 Exercises
- **65 built-in exercises** plus your own
- Primary and secondary muscles on an **outline map** (male and female figure)
- Technique and common mistakes
- **Characteristics**: mechanics, discipline, equipment, force
- 🔎 Search, muscle groups and a **filter** by characteristics
- History and personal records for every exercise

</td>
</tr>
<tr>
<td>

### 📊 Progress
- 30-day stats and a weekly chart
- 🔥 Muscle load on the home screen
- 📈 Per-exercise chart: max weight, volume, reps, duration, distance
- 🏆 Personal records
- 🗓️ Month calendar with workouts

</td>
<td>

### 📅 Planning
- Schedule a workout from a template for a date
- Next workout on the home screen
- "Upcoming" in the diary
- 🔔 Reminders for planned workouts, restored after reboot

</td>
</tr>
<tr>
<td>

### 📏 Body
- Weight and measurements: neck, chest, waist, hips, biceps, thigh, calf
- Trend chart for a month, 3 months, a year or all time
- BMI by three formulas
- **Body composition**: body fat (US Navy method), lean mass, FFMI
- Waist risks: waist-to-height and waist-to-hip ratios

</td>
<td>

### 🌗 Everything else
- Light, dark or system theme
- English and Russian UI
- 💾 Export and import all data as JSON
- App version matches the GitHub release tag

</td>
</tr>
</table>

---

## Tech stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Material3](https://img.shields.io/badge/Material_3-757575?logo=materialdesign&logoColor=white)
![Room](https://img.shields.io/badge/Room_DB-v8-3DDC84?logo=android&logoColor=white)
![Robolectric](https://img.shields.io/badge/Robolectric-tests-6A1B9A)

| Layer | Technology |
|---|---|
| UI | Jetpack Compose (BOM 2025.09) + Material 3 |
| Database | Room 2.8 · SQLite · auto-migrations v1→v8 |
| Serialization | kotlinx.serialization (JSON backup) |
| Navigation | Navigation Compose 2.9 · type-safe routes |
| Background | Foreground service for the timer · AlarmManager for reminders |
| Preferences | SharedPreferences + StateFlow |
| Tests | JUnit 4 · Robolectric · Compose UI tests · Room migration tests |
| CI/CD | GitHub Actions · tests on every push · signed APK and release on a `vX.Y.Z` tag |

---

## Quick start

```bash
git clone https://github.com/keshtoim/Forge.git
cd Forge
./gradlew assembleDebug
```

> Requirements: JDK 17+, Android SDK 36, Android 8.0+ (API 26)

APK → `app/build/outputs/apk/debug/app-debug.apk`

Tests:

```bash
./gradlew testDebugUnitTest
```

A ready-made APK is available in [releases](https://github.com/keshtoim/Forge/releases).

---

## Architecture

```
app/src/main/java/com/keshtoim/forge/
├── data/
│   ├── db/             # Room: entities, DAOs, migrations (v1→v8), backup
│   ├── BuiltInExercise # Built-in exercise catalog and their muscles
│   ├── ExerciseGuide   # Technique and common mistakes
│   ├── ExerciseTraits  # Exercise characteristics and the filter
│   ├── Body            # BMI, body fat, FFMI, waist risks
│   └── UserPreferences # Figure, theme, height, birth date
├── rest/               # Rest timer and foreground service
├── reminder/           # Planned workout reminders
└── ui/
    ├── workout/        # Home and active workout
    ├── history/        # Diary: workouts and body measurements
    ├── exercises/      # Catalog, exercise details, muscle map
    ├── templates/      # Templates and editor
    ├── plan/           # Workout planning
    ├── progress/       # Per-exercise progress
    ├── profile/        # Profile and body composition
    ├── library/        # Library
    ├── settings/       # Theme, figure, backup
    ├── more/           # "More" section
    ├── onboarding/     # Figure choice on first launch
    └── theme/          # Light and dark palettes
```

A single-layer setup without a DI framework: screen → ViewModel → Room DAO. Built-in exercises are defined in code and the database stores only their key, so catalog updates ship without migrations.

---

## Permissions

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Timer and reminder notifications (Android 13+) |
| `FOREGROUND_SERVICE` · `FOREGROUND_SERVICE_SPECIAL_USE` | Rest timer keeps running while the app is in the background |
| `SCHEDULE_EXACT_ALARM` | Reminder at the exact scheduled time |
| `RECEIVE_BOOT_COMPLETED` | Restore reminders after reboot |
| `VIBRATE` · `WAKE_LOCK` | Rest-over signal with the screen off |

---

<div align="center">

made with ❤️ by [Keshtoim](https://github.com/keshtoim)

</div>
