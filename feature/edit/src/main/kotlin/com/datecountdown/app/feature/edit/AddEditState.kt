package com.datecountdown.app.feature.edit

import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import kotlinx.datetime.Instant

/**
 * UI state emitted by [AddEditStore].
 *
 * Lifecycle:
 *  Loading → Form                     (edit mode: event loaded from repository)
 *  Loading → Form                     (create mode: bootstrapper populates defaults immediately)
 *  Loading → LoadError                (edit mode: event not found or repository failure)
 *  Form    → (labels: Saved / Dismissed)  (user actions; state does not change — component reacts)
 */
sealed interface AddEditState {

  /** Initial state — waiting for the bootstrapper to resolve or load the event. */
  data object Loading : AddEditState

  /**
   * The form is ready for interaction.
   *
   * For **create mode** ([eventId] == null) the fields are populated with defaults.
   * For **edit mode**   ([eventId] != null) the fields are populated from the existing [Event].
   *
   * [hasUnsavedChanges] is derived from whether the current field values differ from the values
   * the form was originally populated with. It is used to show or hide the discard-confirmation
   * dialog (AC-AE-13).
   *
   * [isSaving] is true while [SaveEventUseCase] is in progress. The UI should disable the
   * save button and show an inline spinner during this window.
   *
   * [saveError] carries the error message when [SaveEventUseCase] returns a failure. The UI
   * renders an error snackbar or inline hint. Null when no error is pending.
   */
  data class Form(
    val title: String,
    val targetDateTime: Instant,
    val color: EventColor,
    val icon: EventIcon,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    /**
     * True while the "discard unsaved changes?" confirmation dialog should be shown (AC-AE-10).
     * Set to true by [AddEditStore.Intent.RequestDismiss] when [hasUnsavedChanges] is true;
     * cleared to false by [AddEditStore.Intent.DiscardAndDismiss] and
     * [AddEditStore.Intent.CancelDiscardConfirmation].
     */
    val showDiscardConfirmation: Boolean = false,
  ) : AddEditState

  /**
   * The event to edit was not found in the repository, or a repository error occurred on load.
   *
   * [message] is a developer-oriented description; the Compose UI may display a localized string
   * instead. The user can only dismiss the sheet from this state.
   */
  data class LoadError(val message: String) : AddEditState
}
