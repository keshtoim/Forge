<div align="center">

# Forge

**A strength training log that works between your sets, not instead of them**

[![CI](https://img.shields.io/github/actions/workflow/status/keshtoim/Forge/android.yml?label=CI)](https://github.com/keshtoim/Forge/actions)
[![Release](https://img.shields.io/github/v/release/keshtoim/Forge)](https://github.com/keshtoim/Forge/releases)
![Android 8.0+](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)

[Русский](README.md) · English

</div>

Forge logs your gym sessions: which exercises, how many sets, what weight and how hard it felt. Everything stays on your phone. There's no account, no ads, no subscription, and the app has no internet permission at all.

## How a workout goes

1. **Pick a template** or start an empty workout and add exercises from the catalog.
2. **Log a set.** Last session's weight and reps are already filled in grey. Adjust the numbers and tap "Done". For barbell lifts you can enter the plates per side and Forge adds the bar.
3. **Mark how hard it was.** Five color-coded levels, explained behind the "?" button.
4. **Rest.** The rest timer lives in a notification and vibrates when it's time, even with the screen off.

Close the app mid-workout and it picks up right where you left off.

### Set levels

| | Level | When to use |
|---|---|---|
| ⚪ | Warm-up | Doesn't count toward records and stats |
| 🟢 | Light | Plenty of reps left |
| 🟡 | Medium | Working set, 3–4 reps in reserve |
| 🟠 | Heavy | 1–2 reps in reserve |
| 🔴 | Max | To failure or a record weight |

## What else is inside

**Exercise catalog.** 65 built-in exercises plus your own. Each one has a muscle map (male or female figure), technique notes, common mistakes and characteristics: mechanics, discipline, equipment, force. You can filter the catalog by any combination of them.

**Progress.** Per-exercise charts (weight, volume, reps, duration, distance), personal records, 30-day stats and a muscle load map on the home screen.

**Plans.** Schedule a workout from a template for a date. It shows up in the calendar, on the home screen and under "Upcoming", and you get a reminder on the day.

**Body measurements.** Daily weight and measurements with a trend chart. From these Forge calculates BMI, body fat by the US Navy method, lean mass, FFMI, and waist-to-height and waist-to-hip ratios.

**Your data.** Export everything to JSON and import it back, for example when you move to a new phone.

## Install

Grab the APK from [releases](https://github.com/keshtoim/Forge/releases). Requires Android 8.0 or newer.

Build it yourself:

```bash
git clone https://github.com/keshtoim/Forge.git
cd Forge
./gradlew assembleDebug      # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # tests
```

You'll need JDK 17+ and Android SDK 36.

## For developers

Kotlin, Jetpack Compose with Material 3, Room, Navigation Compose with type-safe routes, and kotlinx.serialization. There's no DI framework: screen → ViewModel → DAO.

```
data/        Room, built-in exercises, body formulas, preferences
rest/        rest timer (foreground service)
reminder/    plan reminders (AlarmManager)
ui/          screens by section: workout, history, exercises, plan, …
```

A few decisions worth knowing:

- **Built-in exercises are defined in code.** The database stores only their key, so muscles, technique notes and characteristics update with the app, no migrations needed.
- **The database schema is versioned.** Migrations are tested against the exported schemas in `app/schemas`.
- **The app version comes from the tag.** Pushing a `vX.Y.Z` tag makes GitHub Actions build a signed APK and publish a release with that version.

Tests run on Robolectric, Compose UI flows included, so they don't need an emulator.

## Permissions

- **Notifications**: the rest timer and workout reminders.
- **Foreground service**: keeps the timer running while the app is in the background.
- **Exact alarms and boot completed**: reminders arrive on time, including after a reboot.
- **Vibration and wake lock**: the rest-over signal with the screen off.
