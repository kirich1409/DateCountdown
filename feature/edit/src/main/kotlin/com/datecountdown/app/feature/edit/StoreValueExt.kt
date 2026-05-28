package com.datecountdown.app.feature.edit

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store

/**
 * Bridges an MVIKotlin [Store] to a Decompose [Value] so that Compose UI can observe state via
 * [com.arkivanov.decompose.extensions.compose.subscribeAsState].
 *
 * The subscription is disposed when [lifecycle] reaches the DESTROYED state (i.e., when the
 * component's slot entry is dismissed), preventing leaks.
 *
 * [Store.asValue] does **not** exist in MVIKotlin 4.4.0 — this is a manual bridge pattern
 * required for all feature components that expose [Value] to Compose.
 *
 * Duplicated from [:feature:counter] as an `internal` helper; each feature module keeps its own
 * copy to avoid adding MVIKotlin + Decompose to [:core:common]'s dependency graph.
 */
internal fun <State : Any> Store<*, State, *>.asValue(lifecycle: Lifecycle): Value<State> {
  val mutableValue = MutableValue(state)
  val disposable = states(observer { mutableValue.value = it })
  lifecycle.doOnDestroy { disposable.dispose() }
  return mutableValue
}
