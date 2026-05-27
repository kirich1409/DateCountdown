package com.datecountdown.app.feature.list

import com.arkivanov.decompose.ComponentContext

/**
 * Component interface for the event-list screen.
 *
 * Output carries user interactions that require navigation, translated to nav actions by
 * [com.datecountdown.app.navigation.RootComponent]. Feature modules must not depend on
 * each other — communication happens exclusively via this Output contract.
 *
 * TODO(#35): implement Store + Compose UI for epic 4.
 */
interface EventListComponent {

  /**
   * Navigation outputs emitted by the list screen.
   *
   * RootComponent observes these and translates them to navigation operations on
   * the ChildStack / ChildSlot — no feature module imports another feature's types.
   */
  sealed interface Output {
    /** User tapped an event card — navigate to the counter screen. */
    data class OpenCounter(val eventId: String) : Output

    /** User tapped the FAB / "add" button — open the edit sheet for creation. */
    data object AddEvent : Output

    /** User requested editing an existing event from the list. */
    data class EditEvent(val eventId: String) : Output
  }
}

/**
 * Default stub implementation — component contract only, no Store or UI logic yet.
 *
 * TODO(#35): add MVIKotlin Store and integrate with EventsRepository.
 * Store should be retained via `instanceKeeper.getOrCreate { EventListStore(...) }`.
 */
class DefaultEventListComponent(
  componentContext: ComponentContext,
  // output is consumed by epic 4 Store integration (#35); kept now to establish the contract.
  @Suppress("UnusedPrivateProperty")
  private val output: (EventListComponent.Output) -> Unit,
) : EventListComponent, ComponentContext by componentContext
