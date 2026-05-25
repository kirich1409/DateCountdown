---
type: spec-feature
slug: datecountdown-mvp/navigation
parent: ../README.md
ac_prefix: AC-NAV
modules: [":app", ":domain", ":feature:list", ":feature:counter", ":feature:edit"]
---

# 06 · Навигация

Decompose: `RootComponent` в `:app`. `ChildStack` для основных экранов + `ChildSlot` (overlay) для
edit-sheet. Конфигурации `@Serializable`. Межфичевое взаимодействие — Output-паттерн (feature-компоненты
не зависят друг от друга; RootComponent транслирует Output→навигацию).

## Структура
- **AC-NAV-1** `ChildStack<Config, Child>`: `Config.List`, `Config.Counter(id)`. Тап по карточке списка → `push(Counter(id))`.
- **AC-NAV-2** Edit-sheet — отдельный `ChildSlot<EditConfig, EditChild>` (overlay поверх текущего экрана, НЕ
  выталкивает List/Counter из иерархии). Открытие: `showEdit(id?)` (id=null — создание); закрытие: `dismissEdit()`.
- **AC-NAV-3** System back: при активном edit-slot — закрывает slot (возврат на экран под ним), не трогая stack;
  иначе — pop от ChildStack; на корневом List — стандартный выход из приложения.
- **AC-NAV-4** Состояние навигации (открытый Counter / открытый edit-slot + введённые в него данные) переживает
  поворот экрана и пересоздание процесса (Decompose state restoration + InstanceKeeper). См. [05](05-add-edit.md) AC-AE-14.

## Переходы редактирования
- **AC-NAV-5** Edit со счётчика (иконка edit) → `showEdit(id)` поверх counter; после сохранения slot закрывается, counter обновляется.
- **AC-NAV-6** «Перенести» с past-счётчика → `showEdit(id)`; после сохранения с будущей датой — slot закрывается, counter-компонент пересоздаёт состояние как upcoming (живой тик AC-CL-10 и glass-ряд AC-CL-8 становятся активны). При сохранении с прошлой датой — остаётся past-счётчик ([03](03-past-events.md) AC-PE-13a). API компонента counter — один Output `onEdit(id)` (режим определяется предзаполнением).

## Deep-link из уведомления
- **AC-NAV-7** Alarm-receiver (`:app`) формирует explicit Intent на MainActivity с `EXTRA_EVENT_ID`. RootComponent
  при получении нового intent валидирует id через `EventsRepository`: если событие есть — `push(Counter(id))`
  (с учётом производного статуса на момент тапа: past → past-счётчик [03](03-past-events.md), иначе обычный [02](02-counter-logic.md));
  если id невалиден — остаётся/возвращается на List, без краха.

## Связи
Экраны: [02](02-counter-logic.md), [03](03-past-events.md), [04](04-list-screen.md), [05](05-add-edit.md). Уведомления/intent: [07](07-notifications.md).
