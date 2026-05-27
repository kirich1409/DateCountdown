package com.datecountdown.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.backhandler.BackCallback
import com.datecountdown.app.feature.counter.CounterComponent
import com.datecountdown.app.feature.counter.DefaultCounterComponent
import com.datecountdown.app.feature.edit.AddEditComponent
import com.datecountdown.app.feature.edit.DefaultAddEditComponent
import com.datecountdown.app.feature.list.DefaultEventListComponent
import com.datecountdown.app.feature.list.EventListComponent
import kotlinx.serialization.Serializable

/**
 * Root Decompose component. Lives in [:app], instantiated in [com.datecountdown.app.MainActivity].
 *
 * Holds the primary back-stack ([ChildStack]) for List ↔ Counter navigation (AC-NAV-1) and a
 * separate overlay slot ([ChildSlot]) for the add/edit bottom sheet (AC-NAV-2).
 *
 * **Back-handling (AC-NAV-3):**
 *  1. If the edit slot is active → dismiss it; stack is untouched.
 *  2. Else if the stack has more than one entry → pop the stack.
 *  3. On root List with no slot → callback is disabled; system handles (exits the app).
 *
 * The [BackCallback] is registered at priority DEFAULT+1 so it intercepts before any
 * lower-priority handler installed by the ChildStack itself.
 *
 * **State restoration (AC-NAV-4):**
 *  Both [Config] and [EditConfig] are @Serializable. Decompose persists/restores them via the
 *  platform StateKeeper across rotation and process death.
 *
 * **InstanceKeeper pattern for Stores (epic 4 hook):**
 *  Inside each feature component, retain a Store across configuration changes with:
 *  ```kotlin
 *  val store: MyStore = instanceKeeper.getOrCreate { MyStore(...) }
 *  ```
 *  The Store is destroyed when its back-stack entry is popped, not on configuration change.
 *  See [com.arkivanov.essenty.instancekeeper.InstanceKeeperExt].
 */
class RootComponent(
  componentContext: ComponentContext,
) : ComponentContext by componentContext {

  // ── Primary stack navigation (AC-NAV-1) ──────────────────────────────────────────────────────

  private val stackNavigation = StackNavigation<Config>()

  /**
   * Observable ChildStack. Compose UI subscribes via `stack.subscribeAsState()` from
   * decompose-extensions-compose (added by android-feature convention, transitive to :app).
   */
  val stack: Value<ChildStack<Config, Child>> = childStack(
    source = stackNavigation,
    serializer = Config.serializer(),
    initialConfiguration = Config.List,
    // handleBackButton = false — back is managed manually via BackCallback below so the
    // slot-dismissal can intercept before any stack-pop (AC-NAV-3 priority order).
    handleBackButton = false,
    childFactory = ::createChild,
  )

  // ── Edit-sheet slot navigation (AC-NAV-2) ─────────────────────────────────────────────────────

  private val slotNavigation = SlotNavigation<EditConfig>()

  /**
   * Observable ChildSlot for the add/edit overlay.
   * [ChildSlot.child] is non-null when the sheet is active.
   */
  val editSlot: Value<ChildSlot<EditConfig, EditChild>> = childSlot(
    source = slotNavigation,
    serializer = EditConfig.serializer(),
    initialConfiguration = { null },
    key = "EditSlot",
    handleBackButton = false,
    childFactory = ::createEditChild,
  )

  // ── Back-handling (AC-NAV-3) ──────────────────────────────────────────────────────────────────

  private val backCallback = BackCallback(
    // Priority above DEFAULT so this callback wins over any lower-priority handler.
    priority = BackCallback.PRIORITY_DEFAULT + 1,
    onBack = ::onBack,
  )

  init {
    backHandler.register(backCallback)
    editSlot.subscribe(lifecycle, ObserveLifecycleMode.CREATE_DESTROY) { updateBackCallback() }
    stack.subscribe(lifecycle, ObserveLifecycleMode.CREATE_DESTROY) { updateBackCallback() }
    updateBackCallback()
  }

  private fun updateBackCallback() {
    backCallback.isEnabled = editSlot.value.child != null || stack.value.backStack.isNotEmpty()
  }

  private fun onBack() {
    when {
      editSlot.value.child != null -> slotNavigation.dismiss()
      stack.value.backStack.isNotEmpty() -> stackNavigation.pop()
      // Unreachable: callback is disabled when both conditions are false.
    }
  }

  // ── Public navigation API (called from feature Output translators below) ───────────────────────

  /**
   * Navigate to the counter screen for [eventId] (AC-NAV-1).
   *
   * If [Config.Counter] with this id is already in the stack it is brought to front rather than
   * pushed again, so list-tap and deep-link (AC-NAV-7) both call this method without risk of
   * creating duplicate stack entries.
   */
  fun pushCounter(eventId: String) {
    stackNavigation.pushToFront(Config.Counter(id = eventId))
  }

  /**
   * Open the add/edit overlay (AC-NAV-2).
   * @param eventId the event to edit, or `null` to create a new event.
   */
  fun showEdit(eventId: String? = null) {
    slotNavigation.activate(EditConfig(eventId = eventId))
  }

  /** Dismiss the add/edit overlay (AC-NAV-2). */
  fun dismissEdit() {
    slotNavigation.dismiss()
  }

  // ── Serializable config hierarchies ───────────────────────────────────────────────────────────

  /**
   * Primary navigation configs (AC-NAV-1).
   * @Serializable enables Decompose state persistence across rotation and process death.
   */
  @Serializable
  sealed interface Config {
    @Serializable
    data object List : Config

    @Serializable
    data class Counter(val id: String) : Config
  }

  /**
   * Edit-sheet slot config (AC-NAV-2).
   * [eventId] = null → create; non-null → edit existing.
   */
  @Serializable
  data class EditConfig(val eventId: String?)

  // ── Typed child wrappers ───────────────────────────────────────────────────────────────────────

  sealed interface Child {
    data class ListChild(val component: EventListComponent) : Child
    data class CounterChild(val component: CounterComponent) : Child
  }

  data class EditChild(val component: AddEditComponent)

  // ── Child factories ────────────────────────────────────────────────────────────────────────────

  private fun createChild(config: Config, ctx: ComponentContext): Child =
    when (config) {
      Config.List -> Child.ListChild(
        component = DefaultEventListComponent(
          componentContext = ctx,
          output = ::onListOutput,
        ),
      )
      is Config.Counter -> Child.CounterChild(
        component = DefaultCounterComponent(
          componentContext = ctx,
          eventId = config.id,
          output = ::onCounterOutput,
        ),
      )
    }

  private fun createEditChild(config: EditConfig, ctx: ComponentContext): EditChild =
    EditChild(
      component = DefaultAddEditComponent(
        componentContext = ctx,
        eventId = config.eventId,
        output = ::onEditOutput,
      ),
    )

  // ── Output translators: feature Output → navigation action (Output-pattern) ───────────────────

  private fun onListOutput(output: EventListComponent.Output) {
    when (output) {
      is EventListComponent.Output.OpenCounter -> pushCounter(output.eventId)
      EventListComponent.Output.AddEvent -> showEdit(eventId = null)
      is EventListComponent.Output.EditEvent -> showEdit(output.eventId)
    }
  }

  private fun onCounterOutput(output: CounterComponent.Output) {
    when (output) {
      is CounterComponent.Output.EditEvent -> showEdit(output.eventId)
      CounterComponent.Output.NavigateBack -> stackNavigation.pop()
    }
  }

  private fun onEditOutput(output: AddEditComponent.Output) {
    when (output) {
      AddEditComponent.Output.Saved -> dismissEdit()
      AddEditComponent.Output.Dismissed -> dismissEdit()
    }
  }
}
