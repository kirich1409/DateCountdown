---
type: architecture-contract
slug: module-structure
status: approved
date: 2026-05-27
source: research §2.3 (module structure), §2.4 (applicationId); issues #13–#16
---

# Структура модулей DateCountdown

Контракт границ модулей для всего проекта. Определяет ответственность 9 модулей,
разрешённые направления зависимостей и конвенции именования. Это источник истины для
issue #14 (build-logic + convention plugins), #15 (создание модулей), #16 (applicationId).

**Стек** (см. [`../spikes/1.0-stack-compat.md`](../spikes/1.0-stack-compat.md)): Decompose 3.5.0
компоненты + MVIKotlin 4.4.0 stores + Metro 1.1.1 DI, Compose M3, Room 2.8.4, DataStore,
kotlinx-datetime, KSP 2.3.9, Kotlin 2.3.20, AGP 9.0.1, JDK 17, `minSdk 29` / `targetSdk 36`.

**Производственный package root / applicationId:** `com.datecountdown.app`. Текущий scaffold —
`com.example.datecountdown`; переименование выполняет **issue #16** (необратимо после публикации).
Во всех модулях `namespace` строится от `com.datecountdown.app` (см. таблицу namespace ниже).

---

## 1. Каталог модулей

8 production-модулей (в графе приложения) + `build-logic` (includedBuild, **вне** графа).

### `:domain` — чистый Kotlin: модель и контракты
- **Ответственность:** доменная модель, контракты доступа к данным, чистая бизнес-логика отсчёта.
- **MUST содержать:**
  - `Event`, `EventId`, `EventColor` (enum из 9), `EventIcon` (enum из 16), `ThemeMode` (AC-DM-1..3, AC-DM-9).
  - **Интерфейсы** репозиториев: `EventsRepository` (AC-DM-5/6), `SettingsRepository` (AC-DM-9..11). Реализации — в `:data`.
  - Чистая арифметика отсчёта: `target − now` → годы/дни/часы/минуты/секунды, first-non-zero (primary unit), past-detection (AC-CL-1..3). Функции от (`target`, `now`), через kotlinx-datetime.
  - Чистый выбор quantity-бакета для плюрализации (1 / 2-4 / 5+) — **только выбор бакета**, без строк (AC-L10N-2).
  - Источник тика — pure `Flow<Instant>` 1Гц (AC-CL-11); привязки к жизненному циклу здесь НЕТ.
- **MUST NOT содержать:** любые Android-типы (`Context`, `res`, Compose), Room/DataStore, Decompose/MVIKotlin, Metro-аннотации, Android `plurals`-форматирование.
- **Плагин:** `kotlin-library` (чистый JVM, **не** `android-library`). Это compile-time-гарантия чистоты домена, а не code-review-нит.
- **Стек здесь:** kotlinx-datetime, kotlinx-coroutines (Flow). Ничего больше.

### `:data` — реализации хранения и планирования
- **Ответственность:** персистентность и системные сервисы за контрактами `:domain`.
- **MUST содержать:**
  - Room: `EventEntity`, DAO (`observeEvents(): Flow`, CRUD), database; mapper Entity↔domain (`Long` epoch-millis ↔ `Instant`) (AC-DM-7/8).
  - `EventsRepositoryImpl`, `SettingsRepositoryImpl` (DataStore Preferences) — реализуют интерфейсы из `:domain` (AC-DM-11).
  - `NotificationScheduler` поверх `AlarmManager` (`setExactAndAllowWhileIdle`, `PendingIntent` FLAG_IMMUTABLE/UPDATE_CURRENT, requestCode на `event.id`) (AC-NT-1/6, заголовок 07).
- **MUST NOT содержать:** Compose / UI, Decompose-компоненты, MVIKotlin stores, Metro-граф, **BroadcastReceiver'ы** (alarm-/BOOT-receiver — в `:app`, AC-NT-9), доменную модель или интерфейсы (они в `:domain`).
- **Плагин:** `android-library` + `ksp` (Room compiler).
- **Стек здесь:** Room (+KSP), DataStore, kotlinx-coroutines. **Не** зависит от `:core:design` (Compose не нужен).

### `:core:common` — общие утилиты и форматирование
- **Ответственность:** платформенные утилиты, форматирование, плюрализация-строки, `ClockProvider`.
- **MUST содержать:**
  - `ClockProvider.now(): Instant` (тестируемость now, термин из README).
  - Локализованное форматирование дат/времени (AC-L10N-4).
  - Android `plurals` (RU 1/2-4/5+, EN) — **строковая** плюрализация живёт здесь, не в `:domain` (AC-L10N-2, §2.3 правка ревью). `:domain` отдаёт бакет, `:core:common` отдаёт строку.
- **MUST NOT содержать:** доменную модель/логику, Compose-темы/палитры (это `:core:design`), репозитории, Decompose/MVIKotlin/Metro.
- **Плагин:** `android-library` (нужен `res/values*/plurals.xml`).
- **Стек здесь:** Android `res`, kotlinx-datetime. Может зависеть только от `:domain` при необходимости типов (см. матрицу — допустимо, но не обязательно).

### `:core:design` — дизайн-система Compose
- **Ответственность:** тема, палитры, типографика, фигурные/стеклянные примитивы, переиспользуемые Compose-блоки.
- **MUST содержать:**
  - Material 3 тема: dynamic color (Android 12+) + дизайн-палитра light/dark как fallback (AC-TH-2/3).
  - 9 событийных тональных палитр (`container`/`onContainer`/`hero`/`onHero`), **фиксированные**, не зависят от темы/dynamic (AC-TH-5). Маппинг 16 иконок Material Symbols (AC-DM-3).
  - Типографика M3 + **bundled** шрифты Roboto Flex + Material Symbols Rounded (`res/font`, без сети) (AC-TH-7/8).
  - `BlobShape` (RoundedPolygon) и Glass-эффект (RenderEffect blur API31+ с fallback) (1.11a).
- **MUST NOT содержать:** доменную модель, репозитории, бизнес-логику, Decompose-компоненты, MVIKotlin stores, Metro-граф.
- **Плагин:** `android-library` + `compose`.
- **Решение (не очевидно из спеки): `:core:design` — leaf-модуль, НЕ зависит от `:domain`.** 9 палитр индексируются собственным порядком/enum дизайна; тривиальный маппинг `EventColor → палитра` делают feature-модули. *Почему:* §2.3 объявляет `:core:*` листьями (`:feature:* → :domain, :core:*`); чистота от домена позволяет рендерить Compose-preview примитивов без доменной зависимости и держит дизайн-систему переиспользуемой. Альтернатива (типизировать палитру прямо `EventColor` с зависимостью на `:domain`) отвергнута как нарушающая leaf-инвариант ради косметики.

### `:feature:list` — главный список
- **Ответственность:** список upcoming + секция «Прошедшие», состояния, свайп/undo.
- **MUST содержать:** Decompose `Component` + MVIKotlin `Store` + Compose UI (AC-LS-*, AC-PE-* список). Soft-delete pending в Store (AC-LS-9/10). Карточки **не тикают** посекундно (AC-CL-12 — tick-Flow здесь не используется).
- **MUST NOT содержать:** Room/DataStore/AlarmManager (только через `EventsRepository`/`SettingsRepository` из `:domain`), Metro-граф (компонент получает зависимости через конструктор), зависимости на другие `:feature:*`.
- **Плагин:** `android-feature`.

### `:feature:counter` — полноэкранный счётчик
- **Ответственность:** upcoming-счётчик + past-счётчик, живой тик.
- **MUST содержать:** `Component` + `Store` + Compose (AC-CL-*, AC-PE-*). **Привязка тика к жизненному циклу — только здесь** (Decompose/Essenty `doOnStart`/`doOnStop`, AC-CL-11). Output `onEdit(id)` наверх (AC-NAV-5/6).
- **MUST NOT содержать:** то же, что `:feature:list`.
- **Плагин:** `android-feature`.

### `:feature:edit` — bottom sheet добавления/редактирования
- **Ответственность:** sheet создания/редактирования, живое превью, валидация.
- **MUST содержать:** `Component` + `Store` + Compose (AC-AE-*). Save активен только при непустом trim-названии (AC-AE-7). Сохранность ввода через InstanceKeeper (AC-AE-14).
- **MUST NOT содержать:** то же, что прочие `:feature:*`.
- **Плагин:** `android-feature`.

### `:app` — composition root
- **Ответственность:** Application, MainActivity, корневая навигация, единственная точка сборки DI и экранов, системные приёмники.
- **MUST содержать:**
  - `Application` + `MainActivity` (`ComponentActivity` — нужен `defaultComponentContext()`, gotcha спайка).
  - **Metro root-граф** (`@DependencyGraph` interface) — **единственное место сборки DI** во всём проекте. Граф связывает `EventsRepositoryImpl`/`SettingsRepositoryImpl` (`:data`) с интерфейсами (`:domain`).
  - `RootComponent` (Decompose): `ChildStack<Config, Child>` (`List`, `Counter(id)`) + `ChildSlot` для edit (AC-NAV-1/2). **Трансляция Output фич → навигация** (Output-паттерн, заголовок 06).
  - `BroadcastReceiver`'ы: alarm-receiver (`exported=false`) и BOOT-receiver (`exported=true`, фильтр строго на `BOOT_COMPLETED`) (AC-NT-9/10). Deep-link intent → `push(Counter(id))` с валидацией id (AC-NAV-7).
  - Манифест: `allowBackup=false` + backup-rules, USE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, без INTERNET (AC-DM-12, AC-TH-9), `generateLocaleConfig=true`.
  - Применение темы всего приложения (dynamic color / палитра) и диалог настроек темы (AC-TH-10, без отдельного модуля).
- **MUST NOT содержать:** бизнес-логику (она в `:domain`/Stores фич), прямой доступ к Room/DataStore в обход репозиториев.
- **Плагин:** `android-app` + `compose` + `kotlin-serialization` (персистентность Decompose `Config` sealed-иерархий, R8 keep-правила — 1.13).

### `build-logic` — convention plugins (includedBuild)
- **Ответственность:** общая build-конфигурация. **Вне** графа приложения (`includeBuild("build-logic")` в `settings.gradle.kts`).
- **MUST содержать:** convention-плагины (см. §3), общие настройки AGP 9 DSL, configuration-cache. Детали — issue #14.
- **MUST NOT содержать:** production-код, ресурсы приложения.
- **Особенности:** AGP подключается через `compileOnly`; **не** применять `org.jetbrains.kotlin.android` (несовместим с built-in Kotlin AGP 9 — Kotlin-настройки через AGP DSL); KSP в каталоге как plugin (issue #14).

---

## 2. Правила зависимостей

### Матрица (строка МОЖЕТ зависеть от столбца)

| from \ to        | domain | data | core:common | core:design | feature:* | app |
|------------------|:------:|:----:|:-----------:|:-----------:|:---------:|:---:|
| **:domain**      |   —    |  ✗   |     ✗       |     ✗       |    ✗      |  ✗  |
| **:data**        |   ✓    |  —   |     ✓       |     ✗       |    ✗      |  ✗  |
| **:core:common** |   ✓    |  ✗   |     —       |     ✗       |    ✗      |  ✗  |
| **:core:design** |   ✗    |  ✗   |     ✗       |     —       |    ✗      |  ✗  |
| **:feature:\***  |   ✓    |  ✗   |     ✓       |     ✓       |    ✗      |  ✗  |
| **:app**         |   ✓    |  ✓   |     ✓       |     ✓       |    ✓      |  — |

Одной строкой: `:domain` ← всё; `:data → :domain, :core:common`; `:core:common → :domain`; `:core:design` — leaf; `:feature:* → :domain, :core:common, :core:design`; `:app → всё`.

### Правила и обоснования

- **R1. `:domain` не зависит ни от чего проектного и от Android.** *Почему:* чистый JVM → unit-тесты на полной скорости без инструментирования; независимость от фреймворков — compile-time-инвариант (плагин `kotlin-library`), а не договорённость.
- **R2. `:data → :domain` (реализует интерфейсы), `:data → :core:common`; НЕ `:core:design`.** *Почему:* инверсия зависимостей — детали хранения зависят от контрактов, не наоборот. Compose в data-слой не нужен.
- **R3. `:core:*` — листья; `:core:design` не зависит от `:domain`.** *Почему:* максимум переиспользования; нельзя втягивать домен/данные в дизайн-preview или утилиту. (`:core:common → :domain` допустимо лишь ради общих типов, без обратного направления.)
- **R4. `:feature:*` зависят только от `:domain` + `:core:*`. Фичи НЕ видят `:data`** — только интерфейсы репозиториев из `:domain`. *Почему:* фичи тестируются и заменяются изолированно; асимметрия «`:app` видит `:data`, фичи — нет» — то, ради чего существует всё слоирование.
- **R5. NO feature → feature.** Межфичевое взаимодействие — **Output-паттерн**: feature-компонент эмитит Output, `RootComponent` в `:app` транслирует его в навигацию (заголовок 06, AC-NAV-1/2). *Почему:* предотвращение циклов, параллелизм сборки, каждая фича заменяема в изоляции. Без Output-механизма правило выглядело бы неработающим — он делает его осуществимым.
- **R6. Сборка DI-графа (Metro) — только в `:app`.** Единственный composition root. Фичи и `:data` получают зависимости через конструктор и **не** содержат Metro-аннотаций. *Почему:* фичи остаются DI-framework-agnostic → переиспользуемы и тестируемы без Metro; одна точка владения временем жизни графа (отделена от lifecycle Decompose, см. спайк).
- **R7. NO cycles.** Граф ацикличен по построению (матрица — нижнетреугольная относительно слоёв). *Почему:* циклы ломают инкрементальную и параллельную сборку и делают модули нераспутываемыми.

---

## 3. Конвенции именования

### Gradle-пути и namespace
- Gradle-пути зафиксированы планом: `:core:common`, `:core:design`, `:data`, `:domain`, `:feature:list`, `:feature:counter`, `:feature:edit`, `:app`. `build-logic` — includedBuild (не `:`-путь).
- Package root / `applicationId`: **`com.datecountdown.app`** (issue #16). `namespace` каждого модуля — от него:

  | Модуль | namespace |
  |---|---|
  | `:app` | `com.datecountdown.app` |
  | `:domain` | `com.datecountdown.app.domain` |
  | `:data` | `com.datecountdown.app.data` |
  | `:core:common` | `com.datecountdown.app.core.common` |
  | `:core:design` | `com.datecountdown.app.core.design` |
  | `:feature:list` | `com.datecountdown.app.feature.list` |
  | `:feature:counter` | `com.datecountdown.app.feature.counter` |
  | `:feature:edit` | `com.datecountdown.app.feature.edit` |

### Convention plugin → модуль

Плагины создаются в **issue #14**, применяются при создании модулей в **issue #15**.
Нормализованные имена (kebab-case); в скобках — синонимы из тел issue #13/#14.

| Модуль | Convention plugin(s) |
|---|---|
| `:app` | `android-app` (`android.application`) + `compose` (`compose.library`) + `kotlin-serialization` (`kotlin.serialization`) |
| `:feature:list` / `:feature:counter` / `:feature:edit` | `android-feature` (= `android-library` + `compose` + общие Decompose/MVIKotlin-зависимости) |
| `:core:design` | `android-library` + `compose` |
| `:core:common` | `android-library` |
| `:data` | `android-library` + `ksp` (Room) |
| `:domain` | `kotlin-library` (чистый Kotlin, без Android-плагина) |
| `build-logic` | includedBuild; AGP через `compileOnly`; `detekt` (1.23.8, compose-rules) применяется проектно; AGP 9 built-in Kotlin сохранён — `builtInKotlin=false` не нужен; если потребуется переопределить версию KGP, механизм — buildscript classpath KGP upgrade (см. `docs/spikes/1.0-stack-compat.md`), не `builtInKotlin=false` |

**Синонимы (флаг для #14):** `android-app` ≡ `android.application`; `compose` ≡ `compose.library`;
`kotlin-serialization` ≡ `kotlin.serialization`. `android-feature` фигурирует только в #13 — это composite
поверх `android-library`+`compose`, а не отдельная база. Каноническая форма для проекта — kebab-case из этой таблицы.

---

## 4. Диаграмма зависимостей

```
                          ┌─────────┐
                          │  :app   │  Metro root graph, RootComponent,
                          │         │  receivers, тема приложения
                          └────┬────┘
            ┌──────────────────┼───────────────┬──────────────┐
            ▼                  ▼                ▼              ▼
     ┌────────────┐    ┌──────────────┐  ┌──────────┐   (видит :data
     │ :feature:* │    │    :data     │  │ :core:*  │    только :app)
     │ list       │    │ Room,DataSt. │  │ common   │
     │ counter    │    │ Scheduler    │  │ design   │
     │ edit       │    └──────┬───────┘  └────┬─────┘
     └─────┬──────┘           │               │
           │  ┌───────────────┘               │
           │  │  ┌────────────────────────────┘ (core:common → domain)
           ▼  ▼  ▼
        ┌──────────┐
        │ :domain  │  pure Kotlin: модель, интерфейсы репозиториев,
        │          │  countdown-арифметика  (зависит: НИ ОТ ЧЕГО)
        └──────────┘

build-logic ── includedBuild ── вне графа; поставляет convention plugins всем модулям
```

Слои сверху вниз: `:app` → (`:feature:*`, `:data`, `:core:*`) → `:domain`. Все стрелки направлены
вниз/внутрь; обратных и горизонтальных (feature↔feature) нет.
