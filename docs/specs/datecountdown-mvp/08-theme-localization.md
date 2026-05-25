---
type: spec-feature
slug: datecountdown-mvp/theme-localization
parent: ../README.md
ac_prefix: AC-TH / AC-L10N
modules: [":core:design", ":core:common", ":domain", ":data", ":app", ":feature:list"]
---

# 08 · Тема, dynamic color, локализация, настройки

Доступность (контраст, fontScale) — сквозная конвенция: [`../../conventions/accessibility.md`](../../conventions/accessibility.md).

## Тема (AC-TH)
- **AC-TH-1** Режимы темы: системная / светлая / тёмная. Выбор хранится (SettingsRepository, [01](01-data-model.md) AC-DM-9) и применяется немедленно.
- **AC-TH-2** На Android 12+ базовая тема (surfaces, primary, secondary…) использует **dynamic color** (Material You).
- **AC-TH-3** На <12 и при недоступности dynamic color — палитра из дизайн-токенов (`m3-app.jsx:3-62`), light/dark.
- **AC-TH-4** minSdk проекта = 29 (Technical Constraints); поэтому fallback-палитра нужна для 29–31, dynamic — 31+ (Android 12 = API 31).
- **AC-TH-5** 9 событийных тональных палитр ([01](01-data-model.md) AC-DM-2) **фиксированы** и НЕ зависят от dynamic color/темы — цвет события постоянен (его идентичность). При этом текст на них проходит контраст (см. конвенцию a11y, правило 4).
- **AC-TH-6** Edge-to-edge: контент отступает от системных баров; полноэкранный счётчик — иммерсивный цветной фон (hero).

## Шрифты (AC-TH)
- **AC-TH-7** Roboto Flex (variable) и Material Symbols Rounded поставляются **в APK (bundled)**; приложение НЕ делает сетевых запросов за шрифтами.
- **AC-TH-8** Числа счётчика — Roboto Flex, крупные веса; иконки — Material Symbols Rounded (fill для выбранных/событийных).

## Сеть / приватность (AC-TH)
- **AC-TH-9** Приложение не выполняет runtime сетевых запросов; разрешение `android.permission.INTERNET`
  отсутствует в манифесте (проверяемо grep по итоговому манифесту). Согласуется с Data safety «данные не передаются» ([01](01-data-model.md) AC-DM-12).

## Локализация (AC-L10N)
- **AC-L10N-1** Все пользовательские строки доступны на русском и английском; язык следует системному и
  per-app language picker (Android 13+, `generateLocaleConfig=true`).
- **AC-L10N-2** Русская плюрализация по правилу 1/2-4/5+ (1 день / 2 дня / 5 дней; 1 событие / 2 события / 5 событий),
  через Android `plurals` (`:core:common`); применяется в сабтайтле списка, карточках, счётчике, past-строках.
- **AC-L10N-3** Английская плюрализация корректна (1 day / 2 days; 1 event / 2 events).
- **AC-L10N-4** Даты/время форматируются локализованно («09 МАЯ» / «09 MAY», «30 апреля 2026 · 11:00»).

## Настройки (AC-TH)
- **AC-TH-10** Доступ к выбору темы — пункт «Настройки» в `more_vert` списка ([04](04-list-screen.md) AC-LS-19), открывающий **диалог/bottom-sheet** с выбором из 3 режимов (без отдельного экрана/модуля в MVP). Выбор пишется в SettingsRepository ([01](01-data-model.md) AC-DM-9), применяется немедленно (AC-TH-1). dynamic color/палитра темы применяются на уровне `:app` (тема всего приложения).

## Связи
Палитры/иконки: [01](01-data-model.md). Использование везде в UI. A11y контраст/шрифт: [конвенция](../../conventions/accessibility.md).
