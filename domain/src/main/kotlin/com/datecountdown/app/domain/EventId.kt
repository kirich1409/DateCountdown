package com.datecountdown.app.domain

/**
 * Stable identifier for an [Event].
 *
 * Wraps a UUID string. The value is assigned once at creation and never changes
 * across edits — preserving identity as required by AC-DM-1.
 */
@JvmInline
value class EventId(val value: String)
