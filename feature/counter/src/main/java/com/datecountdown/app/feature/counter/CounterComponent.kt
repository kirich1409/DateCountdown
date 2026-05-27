package com.datecountdown.app.feature.counter

import com.arkivanov.decompose.ComponentContext

/**
 * Component interface for the fullscreen event-counter screen.
 *
 * Per AC-NAV-5/6: the counter screen can trigger opening the edit sheet for the displayed event.
 * Navigation is handled by RootComponent translating Output → ChildSlot activation; this module
 * has no dependency on :feature:edit or any other feature module.
 *
 * TODO(#39): implement Store + Compose UI for epic 4.
 */
interface CounterComponent {

  /** The id of the event being displayed. Supplied at creation time by RootComponent. */
  val eventId: String

  /**
   * Navigation outputs from the counter screen.
   */
  sealed interface Output {
    /** User tapped "edit" — open the edit sheet for the current event. */
    data class EditEvent(val eventId: String) : Output

    /** User navigated back (system back or explicit back button). */
    data object NavigateBack : Output
  }
}

/**
 * Default stub implementation.
 *
 * TODO(#39): add MVIKotlin Store retained via `instanceKeeper.getOrCreate { CounterStore(...) }`.
 */
class DefaultCounterComponent(
  componentContext: ComponentContext,
  override val eventId: String,
  // output is consumed by epic 4 Store integration (#39); kept now to establish the contract.
  @Suppress("UnusedPrivateProperty")
  private val output: (CounterComponent.Output) -> Unit,
) : CounterComponent, ComponentContext by componentContext
