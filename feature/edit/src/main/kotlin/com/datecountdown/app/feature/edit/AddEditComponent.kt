package com.datecountdown.app.feature.edit

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.ExactAlarmPermissionChecker
import com.datecountdown.app.domain.usecase.GetEventUseCase
import com.datecountdown.app.domain.usecase.SaveEventUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Component interface for the add/edit bottom-sheet overlay.
 *
 * Per AC-NAV-2: this lives in a [ChildSlot][com.arkivanov.decompose.router.slot.ChildSlot] in
 * [com.datecountdown.app.navigation.RootComponent] — it overlays the current List/Counter screen
 * without pushing it off the stack.
 *
 * Per AC-AE-14: user input entered in this sheet survives rotation via [InstanceKeeper] — the
 * [AddEditStore] is retained as a [InstanceKeeper.Instance] inside [DefaultAddEditComponent].
 */
interface AddEditComponent {

  /**
   * The id of the event to edit, or `null` when creating a new event.
   * Supplied at creation time by RootComponent via [EditConfig.eventId].
   */
  val eventId: String?

  /** Observable UI state driven by [AddEditStore]. */
  val state: Value<AddEditState>

  /** User changed the title field. */
  fun onTitleChange(title: String)

  /** User changed the target date/time. */
  fun onTargetDateTimeChange(dateTime: Instant)

  /** User selected a different color. */
  fun onColorChange(color: EventColor)

  /** User selected a different icon. */
  fun onIconChange(icon: EventIcon)

  /**
   * User tapped the Save button.
   *
   * Triggers [SaveEventUseCase]; emits [Output.Saved] when the use case completes successfully.
   */
  fun onSaveClick()

  /**
   * User tapped Back or the sheet's dismiss affordance.
   *
   * If there are no unsaved changes the Store emits [AddEditStore.Label.Dismissed], which
   * translates to [Output.Dismissed]. If there are unsaved changes the Store sets
   * [AddEditState.Form.showDiscardConfirmation] = true; the UI observes this via [state] and
   * shows a confirmation dialog (AC-AE-10).
   */
  fun onDismissRequest()

  /**
   * User confirmed they want to discard changes and close the sheet (AC-AE-13).
   *
   * Call this when the user taps "Discard" / "Да" in the confirmation dialog. Clears
   * [AddEditState.Form.showDiscardConfirmation] then emits [Output.Dismissed].
   */
  fun onDiscardConfirmed()

  /**
   * User tapped "Нет" (cancel) in the discard-confirmation dialog (AC-AE-10).
   *
   * Clears [AddEditState.Form.showDiscardConfirmation]; the sheet remains open.
   */
  fun onDismissConfirmCancel()

  /**
   * User confirmed they want to save without scheduling a notification (AC-NT-13).
   *
   * Called from the "Save without notification" button in the exact-alarm-denied dialog.
   * Triggers [SaveEventUseCase] with scheduling disabled; emits [Output.Saved] on success.
   */
  fun onSaveWithoutNotification()

  /**
   * User closed the "exact alarms disabled" dialog without choosing an action (AC-NT-13).
   *
   * Clears [AddEditState.Form.exactAlarmDenied]; the sheet remains open. The user must tap
   * Save again after granting permission (via the dialog's "Open settings" button) or invoke
   * [onSaveWithoutNotification].
   */
  fun onExactAlarmDialogDismiss()

  /**
   * Navigation outputs from the edit sheet.
   *
   * Translated to navigation actions by [com.datecountdown.app.navigation.RootComponent].
   */
  sealed interface Output {
    /** User saved the event (create or update). RootComponent dismisses the slot. */
    data object Saved : Output

    /** User dismissed the sheet without saving. RootComponent dismisses the slot. */
    data object Dismissed : Output
  }
}

/**
 * Default production implementation of [AddEditComponent].
 *
 * The [AddEditStore] is retained across configuration changes via [instanceKeeper.getOrCreate]:
 * the Store is destroyed only when the slot entry is dismissed, not on rotation (AC-AE-14).
 *
 * Label subscription (navigation-level labels only):
 *  [AddEditStore.Label.Saved]     → [output] with [AddEditComponent.Output.Saved]
 *  [AddEditStore.Label.Dismissed] → [output] with [AddEditComponent.Output.Dismissed]
 *
 * The discard-confirmation dialog is driven by [AddEditState.Form.showDiscardConfirmation] in
 * [state] — the Compose UI reads state directly without a separate label subscription (AC-AE-10).
 */
@Suppress("LongParameterList")
class DefaultAddEditComponent(
  componentContext: ComponentContext,
  override val eventId: String?,
  storeFactory: StoreFactory,
  getEvent: GetEventUseCase,
  saveEvent: SaveEventUseCase,
  exactAlarmChecker: ExactAlarmPermissionChecker,
  clock: Clock = Clock.System,
  private val output: (AddEditComponent.Output) -> Unit,
) : AddEditComponent, ComponentContext by componentContext {

  /**
   * Retained wrapper: the Store is created once and destroyed with the component (slot dismissal),
   * not on configuration change. [InstanceKeeper.Instance.onDestroy] calls [AddEditStore.dispose]
   * so that the Store's CoroutineExecutor scope is cancelled and all subscriptions are released.
   */
  private val store: AddEditStore = instanceKeeper.getOrCreate {
    object : InstanceKeeper.Instance {
      val store: AddEditStore = AddEditStoreFactory(
        storeFactory = storeFactory,
        eventId = eventId,
        getEvent = getEvent,
        saveEvent = saveEvent,
        exactAlarmChecker = exactAlarmChecker,
        clock = clock,
      ).create()

      override fun onDestroy() {
        store.dispose()
      }
    }
  }.store

  override val state: Value<AddEditState> = store.asValue(lifecycle)

  init {
    // Translate navigation-level labels to the output callback.
    store.labels(
      observer { label ->
        when (label) {
          AddEditStore.Label.Saved -> output(AddEditComponent.Output.Saved)
          AddEditStore.Label.Dismissed -> output(AddEditComponent.Output.Dismissed)
        }
      },
    )
  }

  override fun onTitleChange(title: String) {
    store.accept(AddEditStore.Intent.UpdateTitle(title = title))
  }

  override fun onTargetDateTimeChange(dateTime: Instant) {
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(dateTime = dateTime))
  }

  override fun onColorChange(color: EventColor) {
    store.accept(AddEditStore.Intent.UpdateColor(color = color))
  }

  override fun onIconChange(icon: EventIcon) {
    store.accept(AddEditStore.Intent.UpdateIcon(icon = icon))
  }

  override fun onSaveClick() {
    store.accept(AddEditStore.Intent.Save)
  }

  override fun onDismissRequest() {
    store.accept(AddEditStore.Intent.RequestDismiss)
  }

  override fun onDiscardConfirmed() {
    store.accept(AddEditStore.Intent.DiscardAndDismiss)
  }

  override fun onDismissConfirmCancel() {
    store.accept(AddEditStore.Intent.CancelDiscardConfirmation)
  }

  override fun onSaveWithoutNotification() {
    store.accept(AddEditStore.Intent.ConfirmSaveWithoutNotification)
  }

  override fun onExactAlarmDialogDismiss() {
    store.accept(AddEditStore.Intent.DismissExactAlarmDialog)
  }
}
