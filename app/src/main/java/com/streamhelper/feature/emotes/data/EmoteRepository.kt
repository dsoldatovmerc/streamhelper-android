package com.streamhelper.feature.emotes.data

import android.util.Log
import com.streamhelper.core.coroutines.IoDispatcher
import com.streamhelper.core.session.LogoutHook
import com.streamhelper.core.session.PostLoginHook
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PostLoginHook, LogoutHook {

    private val _emotes = MutableStateFlow(EmoteSets.Empty)

    /** Both sets; each is replaced as soon as it loads. Empty until then and after logout. */
    val emotes: StateFlow<EmoteSets> = _emotes.asStateFlow()

    // Bumped on logout so a load that was in flight doesn't publish a previous account's emotes.
    private val lock = Mutex()
    private var generation = 0L

    override suspend fun onLoggedIn(user: TwitchUser) {
        val loadGeneration = lock.withLock { generation }
        coroutineScope {
            launch {
                val sevenTv = loadOrEmpty("7TV") { sevenTvApi.getUserByTwitchId(user.id).toEmoteMap() }
                publish(loadGeneration) { it.copy(sevenTv = sevenTv) }
            }
            launch {
                val twitch = loadOrEmpty("Twitch") {
                    val global = async { twitchApi.getGlobalEmotes() }
                    val channel = async { twitchApi.getChannelEmotes(user.id) }
                    twitchEmoteMap(global.await(), channel.await())
                }
                publish(loadGeneration) { it.copy(twitch = twitch) }
            }
        }
    }

    override suspend fun onLoggedOut() {
        lock.withLock {
            generation++
            _emotes.value = EmoteSets.Empty
        }
    }

    private suspend fun publish(loadGeneration: Long, change: (EmoteSets) -> EmoteSets) {
        lock.withLock {
            if (loadGeneration == generation) _emotes.update(change)
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
