<div align="center">

# Forge

**A strength training log that works between your sets, not instead of them**

[![CI](https://img.shields.io/github/actions/workflow/status/keshtoim/Forge/android.yml?label=CI)](https://github.com/keshtoim/Forge/actions)
[![Release](https://img.shields.io/github/v/release/keshtoim/Forge)](https://github.com/keshtoim/Forge/releases)
![Android 8.0+](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)

[Русский](README.md) · English

</div>

Forge logs your gym sessions: which exercises, how many sets, what weight and how hard it felt. Everything stays on your phone. There's no account, no ads and no subscription. The app goes online only for the AI assistant if you turn it on, and once a day to check GitHub for updates (you can switch that off). Workout data leaves the phone only when you ask the AI.

## How a workout goes

1. **Pick a template** or start an empty workout and add exercises from the catalog.
2. **Log a set.** Last session's weight and reps are already filled in grey. Adjust the numbers and tap "Done". For barbell lifts you can enter the plates per side: Forge adds the bar and tells you what to load ("load: 25 + 15 + 1.25"). An untouched set shows a double-progression target like "102.5 kg × 7" that you can use with one tap.
3. **Mark how hard it was.** Five color-coded levels, explained behind the "?" button.
4. **Rest.** The rest timer lives in a notification and vibrates when it's time, even with the screen off. Each exercise has its own rest: 3 minutes for compound lifts and 90 seconds for isolation work by default.

Close the app mid-workout and it picks up right where you left off.

**Warm-up and notes.** The "Warm-up" button adds sets at 40, 60 and 80% of the working weight in front of the working sets. An exercise can carry a note ("seat on 4") that shows up every time, and a workout its own ("slept badly").

**Supersets.** Neighbouring exercises in a template can be joined into a superset. The workout screen then shows the whole round, "Finish round" logs every exercise in it, and rest starts only after the round. "One more round" adds a round with the same weights.

### Set levels

| | Level | When to use |
|---|---|---|
| ⚪ | Warm-up | Doesn't count toward records and stats |
| 🟢 | Light | Plenty of reps left |
| 🟡 | Medium | Working set, 3–4 reps in reserve |
| 🟠 | Heavy | 1–2 reps in reserve |
| 🔴 | Max | To failure or a record weight |

## What else is inside

**Exercise catalog.** 116 built-in exercises across every muscle group plus your own. Each one has a muscle map (male or female figure), technique notes, common mistakes and characteristics: mechanics, discipline, equipment, force. You can filter the catalog by any combination of them.

**Progress.** Per-exercise charts (weight, volume, reps, duration, distance), personal records, 30-day stats and a muscle load map on the home screen.

**Plans.** Schedule a workout for a date, from a template or with your own list of exercises. It shows up in the calendar, on the home screen and under "Upcoming", and you get a reminder on the day.

**Body measurements.** Daily weight and measurements with a trend chart. From these Forge calculates BMI, body fat by the US Navy method, lean mass, FFMI, and waist-to-height and waist-to-hip ratios.

**Your data.** Export everything to JSON and import it back, for example when you move to a new phone.

## AI assistant (optional)

It's off by default, and Forge works as usual without it. To turn it on you need your own API key for one of the services: Gemini (Google AI Studio), ChatGPT (OpenAI) or Claude (Anthropic). Once the key is in, Forge loads the models it can use and you pick one. "Test connection" asks the model for an answer and names the problem if there is one: the key, the region, the balance or the model.

- **Chat coach.** The "AI" tag in the workout header opens a chat. The coach sees the current workout, your past results, your notes and body data (figure, height, age, weight with its trend and body composition), so its answers are specific: what weight to take, how long to rest, what to do instead of a busy machine. You can attach up to three photos from the gallery or camera.
- **Set tip.** The "AI tip" button on a set card suggests weight and reps with a short reason; "Apply" fills them into the set.
- **Workout review.** After a workout, "Review this workout" gives three or four lines: what improved, what dropped and what to watch next time.
- **AI program.** Under "Library → Templates" a short form (goal, days, time, level, equipment, limits) lets the AI build a program from the library's exercises. Review it and save it as templates, supersets included.
- **Connection indicator.** The dot on the "AI" tag: green when connected, grey while checking, red when offline or the key was rejected.

Workout data goes only to the service you picked and only when you ask. The key never goes into a data export. The key is stored on the device, encrypted with a key from the Android Keystore.

## Install

Grab the APK from [releases](https://github.com/keshtoim/Forge/releases). Requires Android 8.0 or newer.

After that the app updates itself: "More" shows "Version X is available", Forge downloads the APK, checks its checksum and signature and opens the system installer over the current version, keeping your data. Only release builds update this way: a debug APK from Actions is signed with another key.

Build it yourself:

```bash
git clone https://github.com/keshtoim/Forge.git
cd Forge
./gradlew assembleDebug      # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # tests
```

You'll need JDK 17+ and Android SDK 36.

## For developers

Kotlin, Jetpack Compose with Material 3, Room, Navigation Compose with type-safe routes, and kotlinx.serialization. The AI assistant uses the official SDKs: google-genai, openai-java and anthropic-java. There's no DI framework: screen → ViewModel → DAO.

```
data/        Room, built-in exercises, body formulas, preferences
rest/        rest timer (foreground service)
data/ai/     AI assistant: provider SDKs, settings, coach prompt
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
- **Internet and network state**: the AI assistant, its connection indicator and the update check.
- **Install apps**: to open the system installer for a downloaded update; Android asks once.
