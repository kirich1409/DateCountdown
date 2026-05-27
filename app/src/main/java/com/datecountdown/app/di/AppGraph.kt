package com.datecountdown.app.di

import dev.zacsweers.metro.DependencyGraph

/**
 * Root Metro dependency graph. Assembled once in [com.datecountdown.app.MainActivity].
 *
 * Currently empty — real bindings are added in upcoming epics:
 *   - Epic 2/3 (#25-27, #29): EventsRepositoryImpl, NotificationScheduler bindings
 *
 * Metro generates the implementation at compile-time via its KSP plugin; the graph is
 * instantiated with `createGraph<AppGraph>()` in MainActivity.
 */
@DependencyGraph
interface AppGraph
