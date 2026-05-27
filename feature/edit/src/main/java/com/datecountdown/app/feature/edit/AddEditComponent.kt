package com.datecountdown.app.feature.edit

import com.arkivanov.decompose.ComponentContext

/**
 * Component interface for the add/edit bottom-sheet overlay.
 *
 * Per AC-NAV-2: this lives in a ChildSlot in RootComponent — it overlays the current
 * List/Counter screen without pushing it off the stack.
 *
 * Per AC-AE-14 / AC-NAV-4: user input entered in this sheet survives rotation via
 * InstanceKeeper — the Store will be retained in epic 4.
 *
 * TODO(#41): implement Store + Compose UI for epic 4.
 */
interface AddEditComponent {

  /**
   * The id of the event to edit, or `null` when creating a new event.
   * Supplied at creation time by RootComponent via [EditConfig.eventId].
   */
  val eventId: String?

  /**
   * Navigation outputs from the edit sheet.
   */
  sealed interface Output {
    /** User saved the event (create or update). RootComponent dismisses the slot. */
    data object Saved : Output

    /** User dismissed the sheet without saving. RootComponent dismisses the slot. */
    data object Dismissed : Output
  }
}

/**
 * Default stub implementation.
 *
 * TODO(#41): add MVIKotlin Store retained via `instanceKeeper.getOrCreate { AddEditStore(...) }`.
 * Entered form data must survive rotation through the retained Store state.
 */
class DefaultAddEditComponent(
  componentContext: ComponentContext,
  override val eventId: String?,
  // output is consumed by epic 4 Store integration (#41); kept now to establish the contract.
  @Suppress("UnusedPrivateProperty")
  private val output: (AddEditComponent.Output) -> Unit,
) : AddEditComponent, ComponentContext by componentContext
