<div align="center">

# Forge

### Дневник силовых тренировок без лишнего

[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![CI](https://img.shields.io/github/actions/workflow/status/keshtoim/Forge/android.yml?style=for-the-badge&label=CI)](https://github.com/keshtoim/Forge/actions)
[![Release](https://img.shields.io/github/v/release/keshtoim/Forge?style=for-the-badge)](https://github.com/keshtoim/Forge/releases)

**[🇷🇺 Русский](README.md) · [🇬🇧 English](README.en.md)**

</div>

---

## Зачем ещё один дневник тренировок?

Большинство фитнес-приложений просят аккаунт, показывают рекламу и прячут статистику за подпиской. Forge делает одно: **быстро записать подход и увидеть прогресс**. Без регистрации, рекламы и облака.

- 📴 **100% офлайн**: у приложения нет даже разрешения на интернет
- 🏋️ **Для зала**: ввод подхода в пару касаний, прошлые значения подставляются сами
- 🧍 **Карта мышц**: видно, что нагружает упражнение и что ты качал за месяц
- 🔓 **Открытый код**: без трекеров и аналитики

---

## Возможности

<table>
<tr>
<td width="50%">

### 🏋️ Тренировка
- Шаблоны тренировок с редактором
- Режим фокуса на одном упражнении
- **Ghost-значения**: вес и повторы прошлой тренировки в каждом подходе
- Подходы списком сверху вниз, «+ Подход» под рукой
- **🎨 Уровень подхода**: разминка, лёгкий, средний, тяжёлый, максимум, у каждого свой цвет и подсказка
- Разминочные подходы не идут в рекорды и статистику
- **⚖️ Умный вес**: блины на сторону плюс гриф
- ⏱️ Таймер отдыха в уведомлении (foreground service)
- Активная тренировка восстанавливается после закрытия приложения

</td>
<td width="50%">

### 📚 Упражнения
- **65 встроенных упражнений** и свои
- Основные и вспомогательные мышцы на **контурной карте** (мужская и женская фигура)
- Техника выполнения и частые ошибки
- **Характеристики**: тип, вид, оборудование, тип усилия
- 🔎 Поиск, группы мышц и **фильтр** по характеристикам
- История и личные рекорды по каждому упражнению

</td>
</tr>
<tr>
<td>

### 📊 Прогресс
- Статистика за 30 дней и график по неделям
- 🔥 Нагрузка на мышцы на главной
- 📈 График по упражнению: максимальный вес, объём, повторы, время, дистанция
- 🏆 Личные рекорды
- 🗓️ Календарь месяца с тренировками

</td>
<td>

### 📅 Планирование
- Запланировать тренировку по шаблону на дату
- Ближайшая тренировка на главной
- «Предстоящие» в дневнике
- 🔔 Напоминания о запланированных тренировках, в том числе после перезагрузки

</td>
</tr>
<tr>
<td>

### 📏 Тело
- Вес и обхваты: шея, грудь, талия, бёдра, бицепс, бедро, голень
- График динамики за месяц, 3 месяца, год или всё время
- ИМТ по трём формулам
- **Состав тела**: процент жира (метод ВМС США), сухая масса, FFMI
- Риски по талии: отношение к росту и к бёдрам

</td>
<td>

### 🌗 Остальное
- Светлая, тёмная или системная тема
- Русский и английский интерфейс
- 💾 Экспорт и импорт всех данных в JSON
- Версия приложения совпадает с тегом релиза на GitHub

</td>
</tr>
</table>

---

## Стек технологий

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Material3](https://img.shields.io/badge/Material_3-757575?logo=materialdesign&logoColor=white)
![Room](https://img.shields.io/badge/Room_DB-v8-3DDC84?logo=android&logoColor=white)
![Robolectric](https://img.shields.io/badge/Robolectric-tests-6A1B9A)

| Слой | Технология |
|---|---|
| UI | Jetpack Compose (BOM 2025.09) + Material 3 |
| БД | Room 2.8 · SQLite · автомиграции v1→v8 |
| Сериализация | kotlinx.serialization (бэкап в JSON) |
| Навигация | Navigation Compose 2.9 · типобезопасные маршруты |
| Фон | Foreground service для таймера · AlarmManager для напоминаний |
| Настройки | SharedPreferences + StateFlow |
| Тесты | JUnit 4 · Robolectric · Compose UI tests · тесты миграций Room |
| CI/CD | GitHub Actions · тесты на каждый push · подписанный APK и релиз по тегу `vX.Y.Z` |

---

## Быстрый старт

```bash
git clone https://github.com/keshtoim/Forge.git
cd Forge
./gradlew assembleDebug
```

> Требования: JDK 17+, Android SDK 36, Android 8.0+ (API 26)

APK → `app/build/outputs/apk/debug/app-debug.apk`

Тесты:

```bash
./gradlew testDebugUnitTest
```

Готовый APK лежит в [релизах](https://github.com/keshtoim/Forge/releases).

---

## Архитектура

```
app/src/main/java/com/keshtoim/forge/
├── data/
│   ├── db/             # Room: сущности, DAO, миграции (v1→v8), бэкап
│   ├── BuiltInExercise # Справочник встроенных упражнений и их мышцы
│   ├── ExerciseGuide   # Техника и частые ошибки
│   ├── ExerciseTraits  # Характеристики упражнений и фильтр
│   ├── Body            # ИМТ, процент жира, FFMI, риски по талии
│   └── UserPreferences # Фигура, тема, рост, дата рождения
├── rest/               # Таймер отдыха и foreground service
├── reminder/           # Напоминания о запланированных тренировках
└── ui/
    ├── workout/        # Главная и активная тренировка
    ├── history/        # Дневник: тренировки и замеры тела
    ├── exercises/      # Каталог, описание упражнения, карта мышц
    ├── templates/      # Шаблоны и редактор
    ├── plan/           # Планирование тренировки
    ├── progress/       # Прогресс по упражнениям
    ├── profile/        # Профиль и состав тела
    ├── library/        # Справочники
    ├── settings/       # Тема, фигура, бэкап
    ├── more/           # Раздел «Ещё»
    ├── onboarding/     # Выбор фигуры при первом запуске
    └── theme/          # Палитры светлой и тёмной темы
```

Однослойная схема без DI-фреймворка: экран → ViewModel → DAO Room. Встроенные упражнения описаны в коде, в базе хранится только ключ, поэтому обновления справочника приходят без миграций.

---

## Разрешения

| Разрешение | Зачем |
|---|---|
| `POST_NOTIFICATIONS` | Уведомления таймера и напоминаний (Android 13+) |
| `FOREGROUND_SERVICE` · `FOREGROUND_SERVICE_SPECIAL_USE` | Таймер отдыха работает, пока приложение свёрнуто |
| `SCHEDULE_EXACT_ALARM` | Напоминание точно в назначенное время |
| `RECEIVE_BOOT_COMPLETED` | Восстановление напоминаний после перезагрузки |
| `VIBRATE` · `WAKE_LOCK` | Сигнал об окончании отдыха при выключенном экране |

---

<div align="center">

сделано с ❤️ by [Keshtoim](https://github.com/keshtoim)

</div>
