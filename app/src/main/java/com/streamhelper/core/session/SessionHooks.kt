package com.streamhelper.core.session

import android.util.Log
import com.streamhelper.core.coroutines.ApplicationScope
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Runs after the Twitch user has loaded, on both app start (Entry) and fresh login.
 * Register with `@Binds @IntoSet`. Each hook runs in its own coroutine on the app scope,
 * fire-and-forget: it doesn't delay navigation, and a failure is logged without affecting others.
 */
fun interface PostLoginHook {
    suspend fun onLoggedIn(user: TwitchUser)
}

/**
 * Runs on logout after stored credentials and [TwitchSession] are cleared, whether or not
 * Twitch revoke succeeded. Register with `@Binds @IntoSet`. Hooks run sequentially and are
 * awaited before logout completes; keep them quick (clear state, close sockets).
 */
fun interface LogoutHook {
    suspend fun onLoggedOut()
}

/** Declares the (possibly empty) hook sets so injection works before any ticket registers one. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SessionHooksModule {
    @Multibinds
    abstract fun postLoginHooks(): Set<PostLoginHook>

    @Multibinds
    abstract fun logoutHooks(): Set<LogoutHook>
}

/** Dispatches registered [PostLoginHook]s and [LogoutHook]s. Used by the auth flow only. */
@Singleton
class SessionHooks @Inject constructor(
    private val postLoginHooks: Set<@JvmSuppressWildcards PostLoginHook>,
    private val logoutHooks: Set<@JvmSuppressWildcards LogoutHook>,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    fun dispatchPostLogin(user: TwitchUser) {
        postLoginHooks.forEach { hook ->
            appScope.launch {
                runCatchingLogged(hook) { hook.onLoggedIn(user) }
            }
        }
    }

    suspend fun runLogoutHooks() {
        logoutHooks.forEach { hook -> runCatchingLogged(hook) { hook.onLoggedOut() } }
    }

    private suspend fun runCatchingLogged(hook: Any, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Session hook ${hook::class.java.name} failed", e)
        }
    }

    private companion object {
        const val TAG = "SessionHooks"
    }
}
