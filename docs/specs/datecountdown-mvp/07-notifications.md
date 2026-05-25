---
type: spec-feature
slug: datecountdown-mvp/notifications
parent: ../README.md
ac_prefix: AC-NT
modules: [":data", ":app"]
risk_areas: [exact-alarm-permission, receiver-security, pii]
---

# 07 · Уведомления

Одно точное уведомление в момент наступления события. `NotificationScheduler` (над AlarmManager) — в
`:data`; alarm-receiver и BOOT-receiver — в `:app`. Без полей уведомления в модели ([01](01-data-model.md)).

## Планирование
- **AC-NT-1** Для каждого upcoming-события планируется ровно одно точное уведомление на `targetDateTime`
  (`AlarmManager.setExactAndAllowWhileIdle`).
- **AC-NT-2** Разрешение точного времени — **USE_EXACT_ALARM** (Android 12+, exempt, без UI-флоу в настройки);
  use case (alarm/reminder) декларируется в Play exact-alarm policy.
- **AC-NT-3** При создании/редактировании события уведомление (пере)планируется; при удалении — отменяется;
  при «Перенести» — переносится на новую дату ([03](03-past-events.md) AC-PE-13).
- **AC-NT-4** Для past-события (на момент создания или после наступления) уведомление не планируется/отменяется.
- **AC-NT-5** Согласование с soft-delete ([04](04-list-screen.md) AC-LS-10): alarm отменяется в момент soft-delete;
  при «Отменить» — перепланируется на исходный `targetDateTime`, если он ещё в будущем.
- **AC-NT-6** `PendingIntent` создаётся с `FLAG_IMMUTABLE` и уникальным `requestCode` на `event.id`
  (иначе alarm разных событий перезатирают друг друга). При пересоздании — `FLAG_UPDATE_CURRENT`.

## Доставка
- **AC-NT-7** Notification channel создаётся при старте; уведомление содержит название события и текст наступления.
- **AC-NT-8** Тап по уведомлению → explicit Intent → полноэкранный счётчик события (deep-link [06](06-navigation.md) AC-NAV-7), с валидацией id.
- **AC-NT-9** Alarm-receiver — `android:exported=false`. BOOT-receiver — `exported=true`, `<intent-filter>` строго
  на `android.intent.action.BOOT_COMPLETED`, внутри валидирует `intent.action`. Разрешение `RECEIVE_BOOT_COMPLETED`.
- **AC-NT-10** После перезагрузки устройства (BOOT_COMPLETED) все будущие уведомления восстанавливаются
  (итерация по upcoming из Room); прошедшие — не планируются.

## Разрешения и обратная связь
- **AC-NT-11** На Android 13+ запрашивается POST_NOTIFICATIONS с пояснением (rationale). Запрос — один раз при
  первом создании события; при отказе автоматически больше не запрашивается; повторный запрос — только через
  явный пункт в настройках приложения.
- **AC-NT-12** При отсутствии разрешения событие всё равно создаётся и видно в списке ([09](09-edge-cases.md) AC-EDGE-5);
  пользователь видит **persistent-баннер на экране списка** «Уведомления выключены — включить» (тап → запрос
  разрешения / системные настройки). Дополнительно пункт «Включить уведомления» в `more_vert` ([04](04-list-screen.md) AC-LS-19).
  Индикатор не блокирует работу; баннер скрывается, когда разрешение выдано.
- **AC-NT-13** Если система отказала в точном alarm (например ограничение/политика) — **без тихой inexact-деградации**:
  при сохранении события показывается модальный диалог с двумя явными выборами: «Открыть настройки» (deeplink) или
  «Сохранить без уведомления». Без явного выбора пользователя событие с уведомлением не сохраняется. *(Decision; правило no-silent-fallback.)*

## Приватность
- **AC-NT-14** На lock-screen — `VISIBILITY_PRIVATE` + `setPublicVersion()` с обобщённым текстом (без названия события).
- **AC-NT-15** Название события не попадает в логи и крэш-отчёты ([01](01-data-model.md) AC-DM-13).

## Связи
Модель: [01](01-data-model.md). Навигация/intent: [06](06-navigation.md). Разрешения в манифесте: см. проектную настройку (research §EPIC5).
