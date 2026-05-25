---
type: spec-feature
slug: datecountdown-mvp/data-model
parent: ../README.md
ac_prefix: AC-DM
modules: [":domain", ":data", ":core:common"]
---

# 01 · Модель данных и репозитории

Доменная модель и контракты доступа к данным. Модель и интерфейсы — в `:domain` (pure Kotlin);
реализации (Room, DataStore) — в `:data`. См. направление зависимостей в [README](README.md).

## Event (домен, `:domain`)
- **AC-DM-1** `Event` имеет поля: `id: EventId` (стабильный, не меняется при редактировании),
  `title: String` (1–60 символов после trim), `targetDateTime: Instant`, `color: EventColor`,
  `icon: EventIcon`, `createdAt: Instant`. Полей уведомления НЕТ (одно уведомление выводится из `targetDateTime`).
- **AC-DM-2** `EventColor` — enum из 9: ORANGE, PINK, BLUE, PURPLE, INDIGO, TEAL, GREEN, RED, AMBER.
  Каждому соответствует тональная палитра (`container`/`onContainer`/`hero`/`onHero`) из `m3-app.jsx:64-85`,
  фиксированная независимо от темы устройства и dynamic color (см. [08](08-theme-localization.md), AC-TH-5).
- **AC-DM-3** `EventIcon` — enum из 16 (Material Symbols Rounded): celebration, cake, beach_access,
  rocket_launch, school, favorite, music_note, directions_run, flight, movie, book_2, spa, restaurant,
  sports_esports, redeem, snowing. Порядок — как в `m3-app.jsx:780` (для пикера, см. [05](05-add-edit.md)).
- **AC-DM-4** `title` хранится trimmed; пустой/из одних пробелов недопустим (см. [05](05-add-edit.md) AC-AE-7).

## EventsRepository (interface в `:domain`, impl в `:data`)
- **AC-DM-5** `observeEvents(): Flow<List<Event>>` — поток всех событий; эмитит при любом изменении.
- **AC-DM-6** `add(event)`, `update(event)`, `delete(id)`, `getById(id): Event?` — корутинные, suspend.
- **AC-DM-7** Реализация — Room (`EventEntity`, DAO, mapper Entity↔domain). `targetDateTime`/`createdAt`
  хранятся как epoch millis (`Long`), конвертируются в `Instant` в mapper. **Стандартный Android Room**, не KMP-Room.
- **AC-DM-8** События переживают перезапуск приложения и устройства (персистентность Room).

## SettingsRepository (interface в `:domain`, impl в `:data` поверх DataStore)
Добавлено по ревью архитектуры: AC-29/AC-57 (тема, свёрнутость) не должны тянуть `:data` в `:feature` напрямую.
- **AC-DM-9** `themeMode: Flow<ThemeMode>` (SYSTEM/LIGHT/DARK) + `setThemeMode(mode)`. См. [08](08-theme-localization.md) AC-TH-1.
- **AC-DM-10** `pastCollapsed: Flow<Boolean>` + `setPastCollapsed(value)`. См. [04](04-list-screen.md) AC-LS-12.
- **AC-DM-11** Реализация — `SettingsRepositoryImpl` на DataStore Preferences в `:data`; интерфейс в `:domain`.

## Приватность данных
- **AC-DM-12** В манифесте: `android:allowBackup="false"` + привязаны `android:dataExtractionRules`
  и `android:fullBackupContent`, исключающие Room DB и DataStore из облачного/device-transfer бэкапа.
  Данные не покидают устройство.
- **AC-DM-13** `title` (потенциальный PII) не попадает в логи и крэш-отчёты (Play Vitals). См. [07](07-notifications.md) AC-NT-15.

## Связи
Используется везде. Цвет/иконка → рендер: [02](02-counter-logic.md), [03](03-past-events.md), [04](04-list-screen.md), [05](05-add-edit.md).
Settings → [04](04-list-screen.md), [08](08-theme-localization.md).
