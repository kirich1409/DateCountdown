package com.datecountdown.app.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.feature.counter.CounterScreen
import com.datecountdown.app.feature.edit.AddEditScreen
import com.datecountdown.app.feature.list.EventListScreen

/**
 * Root composable: wires [RootComponent.stack] and [RootComponent.editSlot] into Compose UI.
 *
 * Primary back-stack ([stack]) is rendered via [Children] with a horizontal slide animation.
 * The add/edit slot ([editSlot]) is rendered as a [ModalBottomSheet] overlay on top of the stack.
 *
 * Note (predictive back): [RootComponent] registers a [com.arkivanov.essenty.backhandler.BackCallback]
 * at [com.arkivanov.essenty.backhandler.BackCallback.PRIORITY_DEFAULT + 1], which takes priority over
 * Decompose's predictive-back animation callback (registered at default priority). The slide animation
 * is therefore used as a non-predictive fallback; predictive-back gesture is still active at the OS
 * level via [android:enableOnBackInvokedCallback="true"] in the manifest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootContent(
  root: RootComponent,
  modifier: Modifier = Modifier,
) {
  Children(
    stack = root.stack,
    modifier = modifier,
    animation = stackAnimation(slide()),
  ) { child ->
    when (val instance = child.instance) {
      is RootComponent.Child.ListChild -> EventListScreen(component = instance.component)
      is RootComponent.Child.CounterChild -> CounterScreen(component = instance.component)
    }
  }

  val editSlot by root.editSlot.subscribeAsState()
  val editChild = editSlot.child?.instance
  if (editChild != null) {
    ModalBottomSheet(
      onDismissRequest = {
        // AC-AE-10: route through the component so unsaved-changes confirmation dialog can
        // intercept the dismiss before the slot is closed.
        editChild.component.onDismissRequest()
      },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      AddEditScreen(component = editChild.component)
    }
  }
}
