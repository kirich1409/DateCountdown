package com.datecountdown.app.feature.edit

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.ExactAlarmPermissionChecker
import com.datecountdown.app.domain.usecase.EventDraft
import com.datecountdown.app.domain.usecase.GetEventUseCase
import com.datecountdown.app.domain.usecase.SaveEventUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext

/**
 * MVIKotlin Store for the add/edit bottom-sheet.
 *
 * Responsibilities:
 *  - In **edit mode** ([eventId] != null): load the existing [Event] on bootstrap and populate
 *    form fields from it (AC-AE-3).
 *  - In **create mode** ([eventId] == null): populate form fields with defaults immediately on
 *    bootstrap without a repository call (AC-AE-5/6).
 *  - Track user-driven field changes and compute [AddEditState.Form.hasUnsavedChanges]
 *    (AC-AE-13).
 *  - Validate and save via [SaveEventUseCase] on [Intent.Save] (AC-AE-9–11).
 *  - Emit navigation labels [Label.Saved] and [Label.Dismissed] for one-shot navigation (AC-AE-9,
 *    AC-AE-13).
 *
 * Public [Intent] and [Label] cross the component boundary.
 * [Message] and [AddEditReducer] are internal implementation details.
 */
internal interface AddEditStore : Store<AddEditStore.Intent, AddEditState, AddEditStore.Label> {

  sealed interface Intent {
    /** User changed the event title text field. */
    data class UpdateTitle(val title: String) : Intent

    /** User selected a different target date/time. */
    data class UpdateTargetDateTime(val dateTime: Instant) : Intent

    /** User selected a different color swatch. */
    data class UpdateColor(val color: EventColor) : Intent

    /** User selected a different icon. */
    data class UpdateIcon(val icon: EventIcon) : Intent

    /**
     * User tapped the Save button.
     *
     * Triggers [SaveEventUseCase]; transitions to [AddEditState.Form.isSaving] = true while in
     * flight. On success emits [Label.Saved]; on failure populates [AddEditState.Form.saveError].
     */
    data object Save : Intent

    /**
     * User confirmed they want to discard changes and close the sheet (AC-AE-13).
     *
     * Clears [AddEditState.Form.showDiscardConfirmation] then emits [Label.Dismissed].
     */
    data object DiscardAndDismiss : Intent

    /**
     * User pressed Back / tapped the dismiss affordance.
     *
     * If [AddEditState.Form.hasUnsavedChanges] == false → emits [Label.Dismissed] immediately.
     * If [AddEditState.Form.hasUnsavedChanges] == true  → sets
     * [AddEditState.Form.showDiscardConfirmation] = true so the UI can show a confirmation
     * dialog without a separate label subscription (AC-AE-10).
     */
    data object RequestDismiss : Intent

    /**
     * User tapped "Нет" (cancel) in the discard-confirmation dialog (AC-AE-10).
     *
     * Clears [AddEditState.Form.showDiscardConfirmation]; no navigation occurs.
     */
    data object CancelDiscardConfirmation : Intent

    /**
     * User confirmed they want to save without scheduling a notification (AC-NT-13).
     *
     * Triggers [SaveEventUseCase] with `scheduleNotification = false`. On success emits
     * [Label.Saved] and clears [AddEditState.Form.exactAlarmDenied].
     */
    data object ConfirmSaveWithoutNotification : Intent

    /**
     * User closed the "exact alarms disabled" dialog without choosing an action (AC-NT-13).
     *
     * Clears [AddEditState.Form.exactAlarmDenied]; the save is NOT performed. The user must
     * tap Save again after granting permission (or use [ConfirmSaveWithoutNotification]).
     */
    data object DismissExactAlarmDialog : Intent
  }

  sealed interface Label {
    /** Event was saved successfully. The sheet should be closed. */
    data object Saved : Label

    /** Sheet should be dismissed without saving. */
    data object Dismissed : Label
  }
}

// ── Internal messages dispatched by the Executor to the Reducer ────────────────────────────────────

private sealed interface Message {
  /** Edit-mode bootstrap succeeded — populate form from the loaded event. */
  data class EventLoaded(val event: Event) : Message

  /** Edit-mode bootstrap failed — event not found. */
  data object EventNotFound : Message

  /** Edit-mode bootstrap failed — repository threw. */
  data class LoadFailed(val cause: Throwable) : Message

  /** Create-mode bootstrap — populate form with defaults. */
  data class DefaultsLoaded(
    val title: String,
    val targetDateTime: Instant,
    val color: EventColor,
    val icon: EventIcon,
  ) : Message

  data class TitleUpdated(val title: String) : Message
  data class TargetDateTimeUpdated(val dateTime: Instant) : Message
  data class ColorUpdated(val color: EventColor) : Message
  data class IconUpdated(val icon: EventIcon) : Message

  data object SaveStarted : Message
  data object SaveSucceeded : Message
  data class SaveFailed(val cause: Throwable) : Message

  /** Reveals the discard-confirmation dialog ([AddEditState.Form.showDiscardConfirmation] = true). */
  data object ShowDiscardConfirmation : Message

  /** Hides the discard-confirmation dialog ([AddEditState.Form.showDiscardConfirmation] = false). */
  data object HideDiscardConfirmation : Message

  /** Reveals the exact-alarm-denied dialog ([AddEditState.Form.exactAlarmDenied] = true). */
  data object ExactAlarmDenied : Message

  /** Hides the exact-alarm-denied dialog ([AddEditState.Form.exactAlarmDenied] = false). */
  data object HideExactAlarmDialog : Message
}

// ── Default form values ─────────────────────────────────────────────────────────────────────────────

private val DEFAULT_COLOR = EventColor.ORANGE
private val DEFAULT_ICON = EventIcon.CELEBRATION

// ── Factory ────────────────────────────────────────────────────────────────────────────────────────

@Suppress("LongParameterList")
internal class AddEditStoreFactory(
  private val storeFactory: StoreFactory,
  private val eventId: String?,
  private val getEvent: GetEventUseCase,
  private val saveEvent: SaveEventUseCase,
  private val exactAlarmChecker: ExactAlarmPermissionChecker,
  private val clock: Clock = Clock.System,
  private val mainContext: CoroutineContext = Dispatchers.Main,
) {

  fun create(): AddEditStore =
    object : AddEditStore,
      Store<AddEditStore.Intent, AddEditState, AddEditStore.Label> by storeFactory.create(
        name = "AddEditStore_${eventId ?: "new"}",
        initialState = AddEditState.Loading,
        bootstrapper = SimpleBootstrapper(Unit),
        executorFactory = {
          Executor(
            eventId = eventId,
            getEvent = getEvent,
            saveEvent = saveEvent,
            exactAlarmChecker = exactAlarmChecker,
            clock = clock,
            mainContext = mainContext,
          )
        },
        reducer = AddEditReducer,
      ) {}
}

// ── Executor ───────────────────────────────────────────────────────────────────────────────────────

private class Executor(
  private val eventId: String?,
  private val getEvent: GetEventUseCase,
  private val saveEvent: SaveEventUseCase,
  private val exactAlarmChecker: ExactAlarmPermissionChecker,
  private val clock: Clock,
  mainContext: CoroutineContext,
) : CoroutineExecutor<AddEditStore.Intent, Unit, AddEditState, Message, AddEditStore.Label>(
  mainContext = mainContext,
) {

  /**
   * Captured during edit-mode bootstrap so that [Message.SaveSucceeded] can build [EventDraft]
   * with the original [EventId] without re-fetching. Not part of public state (AC-AE-3).
   */
  private var originalEvent: Event? = null

  override fun executeAction(action: Unit) {
    bootstrap()
  }

  override fun executeIntent(intent: AddEditStore.Intent) {
    when (intent) {
      is AddEditStore.Intent.UpdateTitle -> dispatch(Message.TitleUpdated(title = intent.title))
      is AddEditStore.Intent.UpdateTargetDateTime -> dispatch(
        Message.TargetDateTimeUpdated(dateTime = intent.dateTime),
      )
      is AddEditStore.Intent.UpdateColor -> dispatch(Message.ColorUpdated(color = intent.color))
      is AddEditStore.Intent.UpdateIcon -> dispatch(Message.IconUpdated(icon = intent.icon))
      AddEditStore.Intent.Save -> save(scheduleNotification = true)
      AddEditStore.Intent.DiscardAndDismiss -> {
        dispatch(Message.HideDiscardConfirmation)
        publish(AddEditStore.Label.Dismissed)
      }
      AddEditStore.Intent.RequestDismiss -> requestDismiss()
      AddEditStore.Intent.CancelDiscardConfirmation -> dispatch(Message.HideDiscardConfirmation)
      AddEditStore.Intent.ConfirmSaveWithoutNotification -> save(scheduleNotification = false)
      AddEditStore.Intent.DismissExactAlarmDialog -> dispatch(Message.HideExactAlarmDialog)
    }
  }

  // ── Private helpers ────────────────────────────────────────────────────────────────────────────

  @Suppress("TooGenericExceptionCaught")
  private fun bootstrap() {
    if (eventId == null) {
      // Create mode: populate with defaults immediately, no I/O needed.
      dispatch(
        Message.DefaultsLoaded(
          title = "",
          targetDateTime = clock.now(),
          color = DEFAULT_COLOR,
          icon = DEFAULT_ICON,
        ),
      )
      return
    }

    scope.launch {
      try {
        val event = getEvent(EventId(eventId))
        if (event == null) {
          dispatch(Message.EventNotFound)
        } else {
          originalEvent = event
          dispatch(Message.EventLoaded(event = event))
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        dispatch(Message.LoadFailed(cause = e))
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun save(scheduleNotification: Boolean) {
    val currentState = state()
    if (currentState !is AddEditState.Form || currentState.isSaving) return

    // Pre-check exact-alarm permission only when we intend to schedule (upcoming event +
    // scheduleNotification flag). Past events skip the check — no alarm will be set.
    val alarmDenied = scheduleNotification
      && currentState.targetDateTime > clock.now()
      && !exactAlarmChecker.canScheduleExactAlarms()

    if (alarmDenied) {
      dispatch(Message.ExactAlarmDenied)
      return
    }

    dispatch(Message.SaveStarted)

    scope.launch {
      try {
        val draft = EventDraft(
          id = originalEvent?.id,
          title = currentState.title,
          targetDateTime = currentState.targetDateTime,
          color = currentState.color,
          icon = currentState.icon,
        )
        saveEvent(draft = draft, scheduleNotification = scheduleNotification)
        dispatch(Message.SaveSucceeded)
        publish(AddEditStore.Label.Saved)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        dispatch(Message.SaveFailed(cause = e))
      }
    }
  }

  private fun requestDismiss() {
    val currentState = state()
    if (currentState is AddEditState.Form && currentState.hasUnsavedChanges) {
      dispatch(Message.ShowDiscardConfirmation)
    } else {
      publish(AddEditStore.Label.Dismissed)
    }
  }
}

// ── Reducer ────────────────────────────────────────────────────────────────────────────────────────

private object AddEditReducer : Reducer<AddEditState, Message> {

  override fun AddEditState.reduce(msg: Message): AddEditState =
    when (msg) {
      is Message.EventLoaded,
      is Message.EventNotFound,
      is Message.LoadFailed,
      is Message.DefaultsLoaded,
      -> reduceBootstrap(msg)

      is Message.TitleUpdated,
      is Message.TargetDateTimeUpdated,
      is Message.ColorUpdated,
      is Message.IconUpdated,
      -> reduceFieldUpdate(msg)

      Message.SaveStarted,
      Message.SaveSucceeded,
      is Message.SaveFailed,
      -> reduceSave(msg)

      Message.ShowDiscardConfirmation -> asForm { copy(showDiscardConfirmation = true) }
      Message.HideDiscardConfirmation -> asForm { copy(showDiscardConfirmation = false) }
      Message.ExactAlarmDenied -> asForm { copy(exactAlarmDenied = true) }
      Message.HideExactAlarmDialog -> asForm { copy(exactAlarmDenied = false) }
    }

  private fun AddEditState.reduceBootstrap(msg: Message): AddEditState =
    when (msg) {
      is Message.EventLoaded -> AddEditState.Form(
        title = msg.event.title,
        targetDateTime = msg.event.targetDateTime,
        color = msg.event.color,
        icon = msg.event.icon,
      )
      is Message.EventNotFound -> AddEditState.LoadError(
        cause = NoSuchElementException("Event not found"),
      )
      is Message.LoadFailed -> AddEditState.LoadError(cause = msg.cause)
      is Message.DefaultsLoaded -> AddEditState.Form(
        title = msg.title,
        targetDateTime = msg.targetDateTime,
        color = msg.color,
        icon = msg.icon,
      )
      else -> this
    }

  private fun AddEditState.reduceFieldUpdate(msg: Message): AddEditState =
    when (msg) {
      is Message.TitleUpdated -> asForm { copy(title = msg.title, hasUnsavedChanges = true) }
      is Message.TargetDateTimeUpdated -> asForm {
        copy(targetDateTime = msg.dateTime, hasUnsavedChanges = true)
      }
      is Message.ColorUpdated -> asForm { copy(color = msg.color, hasUnsavedChanges = true) }
      is Message.IconUpdated -> asForm { copy(icon = msg.icon, hasUnsavedChanges = true) }
      else -> this
    }

  private fun AddEditState.reduceSave(msg: Message): AddEditState =
    when (msg) {
      Message.SaveStarted -> asForm { copy(isSaving = true, saveError = null) }
      Message.SaveSucceeded -> asForm { copy(isSaving = false, saveError = null, exactAlarmDenied = false) }
      is Message.SaveFailed -> asForm { copy(isSaving = false, saveError = msg.cause) }
      else -> this
    }

  /**
   * Applies [transform] only when the current state is [AddEditState.Form].
   * Returns the state unchanged for any other variant — guards against dispatching form messages
   * while in [AddEditState.Loading] or [AddEditState.LoadError].
   */
  private inline fun AddEditState.asForm(
    transform: AddEditState.Form.() -> AddEditState.Form,
  ): AddEditState = if (this is AddEditState.Form) transform() else this
}
