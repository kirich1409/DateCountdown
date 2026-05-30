package com.datecountdown.app

import android.app.Application
import com.datecountdown.app.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

/**
 * Application subclass that owns the Metro [AppGraph] for the lifetime of the process.
 *
 * Moving graph creation here fixes the DataStore / Room singleton crash (bug #138): before this
 * change [MainActivity.onCreate] called [createGraphFactory] on every config-change, producing
 * a new [AppGraph] — and therefore a new DataStore for the same file — on each rotation/theme
 * switch. Keeping the graph in [Application] guarantees exactly one graph per process, which
 * matches the intended [dev.zacsweers.metro.AppScope] lifetime.
 */
class DateCountdownApp : Application() {

  internal val graph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }
}
