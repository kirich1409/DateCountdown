package com.datecountdown.app.feature.counter

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.datecountdown.app.domain.CountdownCalculator
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.PastEventProcessor
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventUseCase

/**
 * Component interface for the fullscreen event-counter screen.
 *
 * Exposes observable [state] (bridged from [CounterStore] via [Value]) and three user-action
 * entry points. Navigation outputs are emitted through [Output] so that [RootComponent] can
 * translate them to navigation calls without creating a dependency between feature modules.
 *
 * AC-NAV-5: edit action opens the add/edit sheet for the current event.
 * AC-NAV-6: back / navigate-back pops the counter off the primary stack.
 */
interface CounterComponent {

  /** Observable UI state driven by [CounterStore]. */
  val state: Value<CounterState>

  /** User tapped the edit button — open the edit sheet for the current event. */
  fun onEditClick()

  /** User pressed the system back button or an explicit back button in the UI. */
  fun onBackClick()

  /** User confirmed deletion of the current event. */
  fun onDeleteClick()

  /**
   * Navigation outputs from the counter screen.
   *
   * Translated to navigation actions by [com.datecountdown.app.navigation.RootComponent].
   */
  sealed interface Output {
    /** Open the edit sheet for [eventId]. */
    data class EditEvent(val eventId: String) : Output

    /** Pop the counter screen from the primary stack. */
    data object NavigateBack : Output
  }
}

/**
 * Default production implementation of [CounterComponent].
 *
 * The [CounterStore] is retained across configuration changes via [instanceKeeper.getOrCreate]:
 * the Store is destroyed only when the back-stack entry is popped, not on rotation.
 *
 * Tick lifecycle (AC-CL-10):
 *  - [doOnStart] fires [CounterStore.Intent.StartTicking] — the 1 Hz ticker starts when the
 *    component becomes visible.
 *  - [doOnStop]  fires [CounterStore.Intent.StopTicking]  — the ticker pauses when the component
 *    goes to the background, conserving resources.
 *
 * Label subscription (delete → navigate back):
 *  Labels are one-shot signals. The subscription is set up once in [init] and lives until the
 *  Store is disposed. [CounterStore.Label.NavigateBack] translates to [Output.NavigateBack].
 */
@Suppress("LongParameterList")
class DefaultCounterComponent(
  componentContext: ComponentContext,
  private val eventId: String,
  storeFactory: StoreFactory,
  getEvent: GetEventUseCase,
  deleteEvent: DeleteEventUseCase,
  calculator: CountdownCalculator,
  pastProcessor: PastEventProcessor,
  private val output: (CounterComponent.Output) -> Unit,
) : CounterComponent, ComponentContext by componentContext {

  /**
   * Retained wrapper: the Store is created once and destroyed with the component (back-stack pop),
   * not on configuration change. [InstanceKeeper.Instance.onDestroy] calls [CounterStore.dispose]
   * so that the Store's CoroutineExecutor scope is cancelled and all subscriptions are released.
   */
  private val store: CounterStore = instanceKeeper.getOrCreate {
    object : InstanceKeeper.Instance {
      val store: CounterStore = CounterStoreFactory(
        storeFactory = storeFactory,
        eventId = EventId(eventId),
        getEvent = getEvent,
        deleteEvent = deleteEvent,
        calculator = calculator,
        pastProcessor = pastProcessor,
      ).create()

      override fun onDestroy() {
        store.dispose()
      }
    }
  }.store

  override val state: Value<CounterState> = store.asValue(lifecycle)

  init {
    // Tick gating: start/stop the 1 Hz ticker with component visibility.
    lifecycle.doOnStart { store.accept(CounterStore.Intent.StartTicking) }
    lifecycle.doOnStop { store.accept(CounterStore.Intent.StopTicking) }

    // Translate Label.NavigateBack (emitted after delete) to the output callback.
    store.labels(
      observer { label ->
        when (label) {
          CounterStore.Label.NavigateBack -> output(CounterComponent.Output.NavigateBack)
        }
      },
    )
  }

  override fun onEditClick() {
    output(CounterComponent.Output.EditEvent(eventId = eventId))
  }

  override fun onBackClick() {
    output(CounterComponent.Output.NavigateBack)
  }

  override fun onDeleteClick() {
    store.accept(CounterStore.Intent.Delete)
  }
}
