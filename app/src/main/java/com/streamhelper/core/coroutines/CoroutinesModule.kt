package com.streamhelper.core.coroutines

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Dispatcher for blocking I/O (disk, DataStore, Keystore crypto). Inject instead of using [Dispatchers.IO]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Dispatcher for CPU-bound work. Inject instead of using [Dispatchers.Default]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Main-thread dispatcher. Inject instead of using [Dispatchers.Main]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * App-lifetime [CoroutineScope] for work that must outlive a single screen
 * (e.g. post-login hooks such as emote loading). Uses a [SupervisorJob] so one failure
 * doesn't cancel siblings.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** The only place in the app that touches `Dispatchers.*` directly; tests replace this module. */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
