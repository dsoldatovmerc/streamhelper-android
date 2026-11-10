package com.streamhelper.feature.emotes.data

import android.util.Log
import com.streamhelper.core.coroutines.IoDispatcher
import com.streamhelper.core.session.LogoutHook
import com.streamhelper.core.session.PostLoginHook
import com.streamhelper.core.session.TwitchSession
import com.streamhelper.core.session.TwitchUser
import com.streamhelper.feature.emotes.Emote
import com.streamhelper.feature.emotes.EmoteSets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * App-scoped emote sets (RN `EmojiStore`): the streamer's 7TV user set and the Twitch
 * global + channel set.
 *
 * Registered as a [PostLoginHook] (loads both sets concurrently after login and on relaunch) and
 * a [LogoutHook] (forgets them). A set that fails to load stays empty and the error is only logged.
 */
@Singleton
class EmoteRepository @Inject constructor(
    private val sevenTvApi: SevenTvApi,
    private val twitchApi: TwitchEmoteApi,
    private val session: TwitchSession,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PostLoginHook, LogoutHook {

    private val _emotes = MutableStateFlow(EmoteSets.Empty)

    /** Both sets; each is replaced as soon as it loads. Empty until then and after logout. */
    val emotes: StateFlow<EmoteSets> = _emotes.asStateFlow()

    // Orders publishing against clearing, so a load still in flight at logout can't publish
    // the previous account's emotes (see [publish]).
    private val lock = Mutex()

    override suspend fun onLoggedIn(user: TwitchUser) {
        coroutineScope {
            launch {
                val sevenTv = loadOrEmpty("7TV") { sevenTvApi.getUserByTwitchId(user.id).toEmoteMap() }
                publish(user) { it.copy(sevenTv = sevenTv) }
            }
            launch {
                val twitch = loadOrEmpty("Twitch") {
                    val global = async { twitchApi.getGlobalEmotes() }
                    val channel = async { twitchApi.getChannelEmotes(user.id) }
                    twitchEmoteMap(global.await(), channel.await())
                }
                publish(user) { it.copy(twitch = twitch) }
            }
        }
    }

    override suspend fun onLoggedOut() {
        lock.withLock { _emotes.value = EmoteSets.Empty }
    }

    /**
     * Applies [change] only while [user] is still signed in. Logout clears [TwitchSession] before
     * running [onLoggedOut], so a publish either sees the session gone and is dropped, or lands
     * first and is wiped by the clear.
     */
    private suspend fun publish(user: TwitchUser, change: (EmoteSets) -> EmoteSets) {
        lock.withLock {
            if (session.userId == user.id) _emotes.update(change)
        }
    }

    private suspend fun loadOrEmpty(
        setName: String,
        load: suspend CoroutineScope.() -> Map<String, Emote>,
    ): Map<String, Emote> =
        try {
            withContext(ioDispatcher) { coroutineScope { load() } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load $setName emotes", e)
            emptyMap()
        }

    private companion object {
        const val TAG = "EmoteRepository"
    }
}
