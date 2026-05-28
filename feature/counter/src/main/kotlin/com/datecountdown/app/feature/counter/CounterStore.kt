package com.datecountdown.app.feature.counter

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.datecountdown.app.domain.CountdownCalculator
import com.datecountdown.app.domain.CountdownResult
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.PastBreakdown
import com.datecountdown.app.domain.PastEventProcessor
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * MVIKotlin Store for the counter screen.
 *
 * Responsibilities:
 *  - Load the event once on bootstrap via [GetEventUseCase].
 *  - Recompute the countdown / past breakdown every ~1 second via a [Tick] loop started and
 *    stopped in response to [StartTicking] / [StopTicking] (gated by Decompose lifecycle in
 *    [DefaultCounterComponent]).
 *  - Handle user-initiated deletion via [DeleteEventUseCase]; emit [Label.NavigateBack] when done.
 *
 * Public Intent and Label hierarchies cross the component boundary. Messages and the reducer are
 * internal implementation details.
 */
internal interface CounterStore : Store<CounterStore.Intent, CounterState, CounterStore.Label> {

  sealed interface Intent {
    /** Load (or reload) the event from the repository. Fired by the bootstrapper automatically. */
    data object LoadEvent : Intent

    /**
     * Supply the current time for a countdown recomputation.
     *
     * Injected by the 1 Hz ticker inside the Executor; also usable in tests for deterministic
     * time control.
     */
    data class Tick(val now: Instant) : Intent

    /**
     * Start the 1 Hz ticker.
     *
     * Called from [DefaultCounterComponent] in [doOnStart]. Idempotent — if the ticker is
     * already active no second ticker is launched.
     */
    data object StartTicking : Intent

    /**
     * Stop the 1 Hz ticker.
     *
     * Called from [DefaultCounterComponent] in [doOnStop]. Safe to call when the ticker is
     * not running.
     */
    data object StopTicking : Intent

    /**
     * Delete the current event and navigate back.
     *
     * Invokes [DeleteEventUseCase] then emits [Label.NavigateBack]. If the state is not
     * [CounterState.Upcoming] or [CounterState.Past] (i.e. event not loaded), this is a no-op.
     */
    data object Delete : Intent
  }

  sealed interface Label {
    /**
     * One-shot signal: the component should invoke the output callback to trigger back
     * navigation in [com.datecountdown.app.navigation.RootComponent].
     */
    data object NavigateBack : Label
  }
}

// ── Internal messages dispatched by the Executor to the Reducer ───────────────────────────────────

private sealed interface Message {
  data object EventNotFound : Message
  data class LoadFailed(val cause: Throwable) : Message

  /**
   * Recomputed countdown / past breakdown for the current [now].
   *
   * [upcoming] is non-null when target > now; [breakdown] is non-null when target ≤ now.
   * Exactly one of the two will be non-null for a given [now].
   */
  data class Recomputed(
    val event: Event,
    val upcoming: CountdownResult.Upcoming?,
    val breakdown: PastBreakdown?,
  ) : Message
}

private const val TICK_INTERVAL_MS = 1_000L

// ── Factory ────────────────────────────────────────────────────────────────────────────────────────

@Suppress("LongParameterList")
internal class CounterStoreFactory(
  private val storeFactory: StoreFactory,
  private val eventId: EventId,
  private val getEvent: GetEventUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val calculator: CountdownCalculator,
  private val pastProcessor: PastEventProcessor,
  private val clock: Clock = Clock.System,
) {

  fun create(): CounterStore =
    object : CounterStore,
      Store<CounterStore.Intent, CounterState, CounterStore.Label> by storeFactory.create(
        name = "CounterStore_${eventId.value}",
        initialState = CounterState.Loading,
        bootstrapper = SimpleBootstrapper(Unit),
        executorFactory = {
          Executor(
            eventId = eventId,
            getEvent = getEvent,
            deleteEvent = deleteEvent,
            calculator = calculator,
            pastProcessor = pastProcessor,
            clock = clock,
          )
        },
        reducer = CounterReducer,
      ) {}
}

// ── Executor ───────────────────────────────────────────────────────────────────────────────────────

private class Executor(
  private val eventId: EventId,
  private val getEvent: GetEventUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val calculator: CountdownCalculator,
  private val pastProcessor: PastEventProcessor,
  private val clock: Clock,
) : CoroutineExecutor<CounterStore.Intent, Unit, CounterState, Message, CounterStore.Label>() {

  private var tickerJob: Job? = null

  override fun executeAction(action: Unit) {
    loadEvent()
  }

  override fun executeIntent(intent: CounterStore.Intent) {
    when (intent) {
      CounterStore.Intent.LoadEvent -> loadEvent()
      is CounterStore.Intent.Tick -> recompute(intent.now)
      CounterStore.Intent.StartTicking -> startTicking()
      CounterStore.Intent.StopTicking -> stopTicking()
      CounterStore.Intent.Delete -> deleteAndNavigateBack()
    }
  }

  // ── Private helpers ──────────────────────────────────────────────────────────────────────────────

  @Suppress("TooGenericExceptionCaught")
  private fun loadEvent() {
    scope.launch {
      try {
        val event = getEvent(eventId)
        if (event == null) {
          dispatch(Message.EventNotFound)
        } else {
          // Compute the first frame immediately using the loaded event so the UI transitions
          // straight to Upcoming/Past without an intermediate re-render of Loading.
          // recomputeFor bypasses the state() read, which would still return Loading here.
          recomputeFor(event = event, now = clock.now())
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        dispatch(Message.LoadFailed(cause = e))
      }
    }
  }

  private fun recompute(now: Instant) {
    val currentState = state()
    val event = when (currentState) {
      is CounterState.Upcoming -> currentState.event
      is CounterState.Past -> currentState.event
      else -> return
    }
    recomputeFor(event = event, now = now)
  }

  private fun recomputeFor(event: Event, now: Instant) {
    val result = calculator.calculate(target = event.targetDateTime, now = now)
    val msg = when (result) {
      is CountdownResult.Upcoming -> Message.Recomputed(
        event = event,
        upcoming = result,
        breakdown = null,
      )
      is CountdownResult.Past -> Message.Recomputed(
        event = event,
        upcoming = null,
        breakdown = pastProcessor.process(target = event.targetDateTime, now = now),
      )
    }
    dispatch(msg)
  }

  private fun startTicking() {
    // Idempotent: do not launch a second ticker if one is already active.
    if (tickerJob?.isActive == true) return
    tickerJob = scope.launch {
      while (true) {
        delay(TICK_INTERVAL_MS)
        recompute(clock.now())
      }
    }
  }

  private fun stopTicking() {
    tickerJob?.cancel()
    tickerJob = null
  }

  @Suppress("TooGenericExceptionCaught")
  private fun deleteAndNavigateBack() {
    val currentState = state()
    // Only proceed when an event is loaded; otherwise the delete is meaningless.
    if (currentState !is CounterState.Upcoming && currentState !is CounterState.Past) return

    scope.launch {
      try {
        deleteEvent(eventId)
        publish(CounterStore.Label.NavigateBack)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        dispatch(Message.LoadFailed(cause = e))
      }
    }
  }
}

// ── Reducer ────────────────────────────────────────────────────────────────────────────────────────

private object CounterReducer : Reducer<CounterState, Message> {

  override fun CounterState.reduce(msg: Message): CounterState =
    when (msg) {
      is Message.EventNotFound -> CounterState.NotFound
      is Message.LoadFailed -> CounterState.Error(cause = msg.cause)
      is Message.Recomputed -> {
        val upcoming = msg.upcoming
        val breakdown = msg.breakdown
        when {
          upcoming != null -> CounterState.Upcoming(event = msg.event, countdown = upcoming)
          breakdown != null -> CounterState.Past(event = msg.event, breakdown = breakdown)
          // Unreachable by construction — Executor always populates exactly one field.
          else -> this
        }
      }
    }
}
