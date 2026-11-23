package com.streamhelper.core.startup

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide startup state used by `MainActivity`:
 * - [isReady]: the Entry auth check has finished and routed; the splash screen stays up until then.
 * - [isFirstActivityCreate]: detects restoration after process death, when restored navigation state
 *   is dropped so the app goes through Entry instead of showing Main with an empty
 *   [com.streamhelper.core.session.TwitchSession].
 */
@Singleton
class StartupGate @Inject constructor() {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var activityCreated = false

    fun markReady() {
        _isReady.value = true
    }

    /** True on the first activity creation in this process (cold start or process death), false after (config changes). */
    @Synchronized
    fun isFirstActivityCreate(): Boolean = !activityCreated.also { activityCreated = true }

    /** Lets `MainActivity` read the gate before `super.onCreate` (i.e. before Hilt field injection). */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Accessor {
        fun startupGate(): StartupGate
    }
}
