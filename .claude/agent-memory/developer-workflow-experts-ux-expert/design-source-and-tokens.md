---
name: design-source-and-tokens
description: Источник истины визуала DateCountdown (m3-app.jsx) — токены, палитры, размеры тач-целей, состояния экранов
type: project
---

Источник истины визуала и логики DateCountdown — `swarm-report/design-source/project/m3-app.jsx` (Material 3 Expressive, из Claude Design bundle). Спеки должны сверяться с ним покомпонентно.

**Why:** UX-ревью спек привязывает AC к конкретным компонентам дизайна; расхождение спеки и макета = partial AC.

**How to apply:** при ревью любой спеки/реализации этого проекта читать m3-app.jsx и сверять компоненты `M3Card` (upcoming/past), `M3CounterTemplate`, `M3CounterPastTemplate`, `M3AddTemplate`, `M3FilterRow`, `M3ScreenEmpty`.

Ключевые факты из дизайна (на 2026-05-25):
- 9 событийных тональных палитр (TONAL_LIGHT/DARK): orange, pink, blue, purple, indigo, teal, green, red, amber. Поля: container/onContainer (карточка), hero/onHero (счётчик). НЕ зависят от dynamic color.
- 16 иконок Material Symbols Rounded. ВНИМАНИЕ: `M3_PAST_SAMPLE` использует `hiking`, которой нет в фикс-списке 16 — это sample-data, не часть набора.
- Тач-цели в макете МЕНЬШЕ 48dp: фильтр-чипы 36dp, color-квадраты 40dp, icon-grid ячейки 40dp. M3IconButton — 48dp (ок).
- Счётчик: primary число fontSize до 220px (180 для years-режима) — риск разрыва layout при крупном fontScale.
- Карточка upcoming показывает чип dateLabel («09 МАЯ») в правом верхнем углу — легко пропустить в AC.
- Past-counter (`M3CounterPastTemplate`) имеет в топбаре `more_vert` И нижнюю кнопку «Удалить» одновременно.
- Upcoming-counter (`M3CounterTemplate`) рисует в топбаре edit + share + more_vert (share вне scope MVP).
- Иконки несут смысл (эмодзи-семантика события) → требуют contentDescription для TalkBack.
