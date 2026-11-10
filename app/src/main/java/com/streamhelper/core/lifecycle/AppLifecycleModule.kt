package com.streamhelper.core.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

/**
 * The whole app's foreground/background [Lifecycle] ([ProcessLifecycleOwner]): STARTED while any
 * activity is visible. Observe `currentStateFlow` from any thread. Tests replace this module with a
 * `TestLifecycleOwner`'s lifecycle.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppLifecycle

@Module
@InstallIn(SingletonComponent::class)
object AppLifecycleModule {
    @Provides
    @AppLifecycle
    fun appLifecycle(): Lifecycle = ProcessLifecycleOwner.get().lifecycle
}
