---
type: spec-index
slug: datecountdown-mvp
date: 2026-05-25
status: approved
platform: android
---

# DateCountdown MVP — Спецификация (индекс)

Приложение обратного отсчёта до заданных дат (Android, Material 3). Эта папка — спецификация
поведения, разбитая по фичам. Каждый файл содержит проверяемые acceptance-критерии (AC) с
префиксом фичи. Документ — контракт для реализации и верификации (`/acceptance`); AC разносятся
по GitHub issues.

## Источники истины (по приоритету)
1. **Скриншоты макапов** — `../screenshots/*.jpg` (визуальный эталон, M3/Android). Карта ниже.
2. **Дизайн-код** — `../../../swarm-report/design-source/project/m3-app.jsx` (точные цвета, размеры, логика).
3. **Эта спецификация** — формализованное поведение + AC.
4. Интент пользователя — `../../../swarm-report/design-source/chats/chat1.md`, `chat3.md`.

При конфликте: скриншот побеждает по визуалу; m3-app.jsx — по числам/токенам; спека — по поведению/edge-cases.

## Файлы
| Файл | Фича | Префикс AC |
|---|---|---|
| [01-data-model.md](01-data-model.md) | Модель данных Event + репозитории | AC-DM |
| [02-counter-logic.md](02-counter-logic.md) | Логика счётчика (остаток, единицы, тик) | AC-CL |
| [03-past-events.md](03-past-events.md) | Прошедшие: карточка + past-счётчик | AC-PE |
| [04-list-screen.md](04-list-screen.md) | Главный список + состояния + свайп/undo | AC-LS |
| [05-add-edit.md](05-add-edit.md) | Bottom sheet добавления/редактирования | AC-AE |
| [06-navigation.md](06-navigation.md) | Навигация (Decompose), deep-link | AC-NAV |
| [07-notifications.md](07-notifications.md) | Уведомления + разрешения + alarm | AC-NT |
| [08-theme-localization.md](08-theme-localization.md) | Тема, dynamic color, i18n, настройки | AC-TH / AC-L10N |
| [09-edge-cases.md](09-edge-cases.md) | Edge-cases и не-цели | AC-EDGE |

## Сквозные конвенции (применяются ко всем фичам, не отдельная фича)
- **Доступность** — [`../../conventions/accessibility.md`](../../conventions/accessibility.md). Обязательна для любого UI (contentDescription, тач-цели ≥48dp, контраст, fontScale, фокус, объявление snackbar). Per-feature AC ссылаются сюда.

## Карта скриншотов → фичи
| Скриншот | Экран | Файл |
|---|---|---|
| `list-default.jpg` | Список, 8 событий | 04 |
| `list-swipe-delete.jpg` | Свайп удаления | 04 |
| `list-with-past.jpg` | Список + секция «Прошедшие» | 04, 03 |
| `empty-state.jpg` | Пустое состояние | 04 |
| `counter-near-3d.jpg` | Счётчик, 3 дня (primary=дни) | 02 |
| `counter-far-248d.jpg` | Счётчик, 248 дней | 02 |
| `counter-today-4h.jpg` | Счётчик, 4 часа (primary=часы, glass=2) | 02 |
| `counter-long-9y253d.jpg` | Счётчик, 9 лет 253 дня (стек) | 02 |
| `counter-past.jpg` | Past-счётчик (−25) | 03 |
| `add-edit-empty.jpg` | Добавление, пустое, Save disabled | 05 |
| `add-edit-filled.jpg` | Добавление, заполнено, Save active | 05 |

## Общие термины
- **now** — текущий момент через `ClockProvider.now(): Instant` (тестируемость).
- **upcoming** — `targetDateTime > now`; **past** — `targetDateTime <= now` (производный статус, не отдельная сущность).
- **primary unit** — первая ненулевая единица сверху вниз: годы→дни→часы→минуты→секунды (см. [02](02-counter-logic.md)).

## Зафиксированные сквозные решения
Стек и инфраструктура — в `../../../swarm-report/research/research-datecountdown-implementation.md`.
Платформа Android-only (KMP-ready); Compose + M3 + dynamic color; Decompose + MVIKotlin + Metro DI;
Room + DataStore; kotlinx-datetime; дата = `Instant`; RU+EN; bundled fonts; `allowBackup=false`;
USE_EXACT_ALARM; одно уведомление в момент события. **Никаких fallback без явного согласия пользователя.**

## Не-цели MVP
Виджет; шаринг; облако/аккаунт; функциональные фильтр-чипы и поиск; несколько/настраиваемые
уведомления; импорт из календаря; повторяющиеся события; screenshot-тесты; RTL; адаптив под
tablet/landscape/foldable. См. [09-edge-cases.md](09-edge-cases.md).
