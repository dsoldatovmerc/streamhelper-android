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
 * Process-wide flag: has the Entry auth check finished in this process? `MainActivity` keeps the
 * splash screen up until it has, and drops restored UI state if it hasn't (process death), so the
 * app always goes through Entry and never shows Main with an empty [com.streamhelper.core.session.TwitchSession].
 */
@Singleton
class StartupGate @Inject constructor() {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun markReady() {
        _isReady.value = true
    }

    /** Lets `MainActivity` read the gate before `super.onCreate` (i.e. before Hilt field injection). */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Accessor {
        fun startupGate(): StartupGate
    }
}
