---
name: module-graph
description: DateCountdown multi-module архитектура — 8 prod-модулей + build-logic, направление зависимостей, fit Decompose×MVIKotlin×Metro, спорные точки размещения слоёв
metadata:
  type: project
---

Android-only KMP-ready countdown-app. Стек зафиксирован (не предлагать замену самих решений): Compose+M3+dynamic color, Decompose (навигация/компоненты), MVIKotlin (Store), Metro DI (compile-time KSP), Room+DataStore, kotlinx-datetime, AlarmManager. Multi-module.

**Why:** showcase reference-architecture поверх 5 экранов; KMP-готовность без введения KMP source sets сейчас.

**How to apply (граф зависимостей — целевой):**
- `:build-logic` — includedBuild (convention plugins), НЕ runtime-модуль. Счёт: 8 production-модулей + build-logic.
- Направление: `:feature:* → :domain, :core:*` ; `:data → :domain, :core:common` ; `:app → всё`. :feature не должны видеть :data напрямую.
- `:domain` — pure Kotlin (без Android). Здесь должны жить: Event domain-model, EventId, Color/Icon enums, EventsRepository **interface**, countdown-арифметика, first-non-zero, past-detection, pure 1Hz tick Flow.
- `:data` — только EventEntity (Room), DAO, EventsRepositoryImpl, mapper, DataStore, NotificationScheduler+channel.
- `:core:common` — универсальные типы + plurals-форматирование (Android-lib, т.к. plurals = Android resources, НЕ может быть в pure :domain).
- `:core:design` — тема, 9 событийных палитр, маппинг Color enum → палитра, типографика. Маппит Color из :domain (не из :data).

**Решённые архитектурные споры (выявлены при ревью плана 2026-05-25):**
- Event/Color/Icon/Repository-interface → :domain, НЕ :data (иначе :feature и :core:design тянут :data, стрелка перевёрнута).
- Plurals-форматирование → :core:common (Android), НЕ pure :domain.
- Tick: pure Flow → :domain; lifecycle-привязка (старт/стоп) → Decompose Lifecycle (Essenty) внутри :feature:counter Store.
- Inter-feature навигация: feature-компоненты НЕ зависят друг от друга, общаются через Output sealed interface; RootComponent в :app переводит Output → push конфигурации. Нужен ADR.
- Notification ownership: scheduler/channel → :data; BroadcastReceiver + deep-link в counter → :app (знает RootComponent + конфиги).

**Корректные решения в плане (не трогать):** Metro×Decompose как plan-stage gate с fallback kotlin-inject; R8 keep для Decompose Config (не NavKey); InstanceKeeper для Store retention; build-logic первым.
