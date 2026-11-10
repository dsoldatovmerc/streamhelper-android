package com.streamhelper.feature.chat.data

import android.util.Log
import androidx.lifecycle.Lifecycle
import com.streamhelper.R
import com.streamhelper.core.coroutines.ApplicationScope
import com.streamhelper.core.lifecycle.AppLifecycle
import com.streamhelper.core.network.TwitchEventSubUrl
import com.streamhelper.core.session.LogoutHook
import com.streamhelper.core.session.TwitchSession
import com.streamhelper.core.snackbar.SnackbarBus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Chat connection as seen by the UI. */
enum class ChatConnectionState {
    /** Not started, in the background, or logged out. */
    Idle,

    /** First connect (from [Idle]) in progress. */
    Connecting,

    /** Welcome received. */
    Connected,

    /** Dropped; reconnecting with backoff until the next welcome. */
    Disconnected,
}

/**
 * App-scoped Twitch chat (RN `ChatStore`): owns the EventSub socket, the `channel.chat.message`
 * subscription and the message list. A singleton so messages survive tab switches.
 *
 * All connection state is mutated by one actor coroutine fed through [inputs], so socket
 * callbacks, timers, lifecycle and user intents never race. Handles `session_reconnect`
 * migration, keepalive timeout, auto-reconnect with exponential backoff, and the process
 * lifecycle (closed in the background, reconnected and re-subscribed in the foreground).
 * Registered as a [LogoutHook]: logout closes the socket and clears the messages.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val socket: EventSubSocket,
    private val api: EventSubApi,
    private val session: TwitchSession,
    private val snackbarBus: SnackbarBus,
    @AppLifecycle private val appLifecycle: Lifecycle,
    @TwitchEventSubUrl private val eventSubUrl: String,
    @ApplicationScope private val appScope: CoroutineScope,
) : LogoutHook {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    /** Immutable snapshots, oldest first; a new list on every change. Unbounded (RN parity, see PLAN.md). */
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow(ChatConnectionState.Idle)
    val connectionState: StateFlow<ChatConnectionState> = _connectionState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)

    /** True from a manual [reconnect] until the subscription is created or the attempt fails. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val inputs = Channel<Input>(Channel.UNLIMITED)

    // Actor state: touched only inside handle().
    private var started = false
    private var foreground = false
    private var active: Connection? = null
    private var pending: Connection? = null // session_reconnect target, until its welcome
    private var nextConnectionId = 0L
    private var retryJob: Job? = null
    private var retryToken = 0L
    private var backoffAttempt = 0
    private var dropNotified = false
    private val seenMessageIds = HashSet<String>()

    init {
        appScope.launch { for (input in inputs) handle(input) }
        appScope.launch {
            appLifecycle.currentStateFlow
                .map { it.isAtLeast(Lifecycle.State.STARTED) }
                .distinctUntilChanged()
                .collect { inputs.send(Input.Foreground(it)) }
        }
    }

    /** Starts chat (Chat tab opened). Idempotent; connects now if the app is in the foreground. */
    fun start() {
        inputs.trySend(Input.Start)
    }

    /** Manual reconnect (pull-to-refresh). No-op while connected. */
    fun reconnect() {
        inputs.trySend(Input.Refresh)
    }

    /**
     * Marks a message as deleted in the list (moderation ticket). No-op for unknown or
     * already-deleted ids. Safe to call from any thread.
     */
    fun markDeleted(messageId: String) {
        _messages.update { list ->
            if (list.none { it.id == messageId && !it.isDeleted }) {
                list
            } else {
                list.map { if (it.id == messageId) it.copy(isDeleted = true) else it }
            }
        }
    }

    /** Logout: close the socket, cancel reconnects, clear messages. Returns once done. */
    override suspend fun onLoggedOut() {
        val done = CompletableDeferred<Unit>()
        inputs.send(Input.Logout(done))
        done.await()
    }

    private fun handle(input: Input) {
        when (input) {
            Input.Start -> {
                started = true
                connect()
            }
            Input.Refresh -> refresh()
            is Input.Foreground -> {
                foreground = input.isForeground
                if (input.isForeground) {
                    connect()
                } else {
                    stop()
                }
            }
            is Input.Logout -> {
                started = false
                stop()
                seenMessageIds.clear()
                _messages.value = emptyList()
                input.done.complete(Unit)
            }
            is Input.Frame -> onFrame(input.connectionId, input.message)
            is Input.Closed -> if (isCurrent(input.connectionId)) {
                Log.w(TAG, "EventSub socket closed", input.error)
                onConnectionLost(input.connectionId)
            }
            is Input.KeepaliveExpired -> if (isCurrent(input.connectionId)) {
                Log.w(TAG, "No EventSub frame within the keepalive timeout")
                onConnectionLost(input.connectionId)
            }
            is Input.SubscriptionResult -> if (input.connectionId == active?.id) {
                _isRefreshing.value = false
                if (input.success) snackbarBus.success(R.string.chat_connection_success)
            }
            is Input.Retry -> if (input.token == retryToken) {
                retryJob = null
                connect()
            }
        }
    }

    private fun refresh() {
        val current = active ?: pending
        when {
            current?.welcomed == true -> Unit // already connected
            current != null -> _isRefreshing.value = true // an attempt is already in flight
            else -> {
                _isRefreshing.value = true
                connect()
            }
        }
    }

    /** Opens a socket if chat is wanted and none is open; cancels any scheduled retry. */
    private fun connect() {
        if (active != null || pending != null) return
        if (!started || !foreground || session.userId == null) {
            _isRefreshing.value = false
            return
        }
        cancelRetry()
        if (_connectionState.value != ChatConnectionState.Disconnected) {
            _connectionState.value = ChatConnectionState.Connecting
        }
        active = open("$eventSubUrl?keepalive_timeout_seconds=$KEEPALIVE_TIMEOUT_SECONDS")
    }

    private fun open(url: String): Connection {
        val connection = Connection(nextConnectionId++)
        connection.job = appScope.launch {
            val error = try {
                socket.connect(url).collect { inputs.send(Input.Frame(connection.id, it)) }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e
            }
            inputs.send(Input.Closed(connection.id, error))
        }
        armKeepalive(connection)
        return connection
    }

    private fun onFrame(connectionId: Long, message: EventSubMessage) {
        val connection = listOfNotNull(active, pending).firstOrNull { it.id == connectionId } ?: return
        armKeepalive(connection)
        when (message) {
            is EventSubMessage.Welcome -> onWelcome(connection, message)
            is EventSubMessage.ChatMessageReceived -> {
                // EventSub is at-least-once and a migration overlaps two sockets: drop repeats,
                // which would also break the list's unique keys.
                if (seenMessageIds.add(message.message.id)) {
                    _messages.update { it + message.message }
                }
            }
            EventSubMessage.Keepalive -> Unit
            is EventSubMessage.Reconnect -> if (connection === active && pending == null) {
                Log.i(TAG, "EventSub session_reconnect; migrating")
                pending = open(message.reconnectUrl)
            }
            is EventSubMessage.Revocation -> {
                Log.w(TAG, "EventSub subscription revoked: ${message.subscriptionType} (${message.status})")
                onDropped()
            }
            is EventSubMessage.Other -> Unit
        }
    }

    private fun onWelcome(connection: Connection, welcome: EventSubMessage.Welcome) {
        welcome.keepaliveTimeoutSeconds?.let {
            connection.keepaliveTimeoutMillis = it * 1000L
            armKeepalive(connection)
        }
        connection.welcomed = true
        backoffAttempt = 0
        dropNotified = false
        _connectionState.value = ChatConnectionState.Connected

        if (connection === pending) {
            // Migration: Twitch carried the subscriptions over, so don't re-create them.
            active?.close()
            active = connection
            pending = null
            _isRefreshing.value = false
            return
        }
        val userId = session.userId
        if (userId == null) {
            Log.w(TAG, "EventSub welcome without a signed-in user; not subscribing")
            _isRefreshing.value = false
            return
        }
        connection.subscriptionJob = appScope.launch {
            val success = try {
                subscribeToChat(userId, welcome.sessionId)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Creating the chat subscription failed", e) // RN parity: log only
                false
            }
            inputs.send(Input.SubscriptionResult(connection.id, success))
        }
    }

    /** Deletes stale subscriptions (fire-and-forget) and subscribes to the streamer's chat. */
    private suspend fun subscribeToChat(userId: String, sessionId: String) {
        api.getSubscriptions().data.forEach { subscription ->
            appScope.launch {
                try {
                    api.deleteSubscription(subscription.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Deleting stale subscription ${subscription.id} failed", e)
                }
            }
        }
        api.createSubscription(CreateSubscriptionRequest.chatMessages(userId, sessionId))
    }

    /** One socket died. During a migration only the other socket survives; otherwise chat dropped. */
    private fun onConnectionLost(connectionId: Long) {
        val survivor = when (connectionId) {
            pending?.id -> active
            active?.id -> pending // Twitch may close the old socket before the new one welcomes us
            else -> null
        }
        if (survivor == null) {
            onDropped()
            return
        }
        listOfNotNull(active, pending).filter { it.id == connectionId }.forEach { it.close() }
        if (survivor.welcomed) {
            active = survivor
            pending = null
        } else {
            active = null
            pending = survivor
        }
    }

    private fun onDropped() {
        closeConnections()
        _connectionState.value = ChatConnectionState.Disconnected
        _isRefreshing.value = false
        if (!dropNotified) {
            dropNotified = true
            snackbarBus.error(R.string.chat_connection_error)
        }
        scheduleRetry()
    }

    private fun scheduleRetry() {
        if (!started || !foreground) return
        cancelRetry()
        val delayMillis = (INITIAL_BACKOFF_MILLIS shl backoffAttempt.coerceAtMost(MAX_BACKOFF_SHIFT))
            .coerceAtMost(MAX_BACKOFF_MILLIS)
        backoffAttempt++
        val token = retryToken
        retryJob = appScope.launch {
            delay(delayMillis)
            inputs.send(Input.Retry(token))
        }
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
        retryToken++
    }

    /** Background or logout: close everything and forget drop/backoff bookkeeping. */
    private fun stop() {
        closeConnections()
        cancelRetry()
        backoffAttempt = 0
        dropNotified = false
        _isRefreshing.value = false
        _connectionState.value = ChatConnectionState.Idle
    }

    private fun closeConnections() {
        active?.close()
        pending?.close()
        active = null
        pending = null
    }

    private fun isCurrent(connectionId: Long) = connectionId == active?.id || connectionId == pending?.id

    private fun armKeepalive(connection: Connection) {
        connection.keepaliveJob?.cancel()
        connection.keepaliveJob = appScope.launch {
            delay(connection.keepaliveTimeoutMillis + KEEPALIVE_GRACE_MILLIS)
            inputs.send(Input.KeepaliveExpired(connection.id))
        }
    }

    private class Connection(val id: Long) {
        var job: Job? = null
        var keepaliveJob: Job? = null
        var subscriptionJob: Job? = null
        var welcomed = false
        var keepaliveTimeoutMillis = KEEPALIVE_TIMEOUT_SECONDS * 1000L

        fun close() {
            job?.cancel()
            keepaliveJob?.cancel()
            subscriptionJob?.cancel()
        }
    }

    private sealed interface Input {
        data object Start : Input
        data object Refresh : Input
        data class Foreground(val isForeground: Boolean) : Input
        data class Logout(val done: CompletableDeferred<Unit>) : Input
        data class Frame(val connectionId: Long, val message: EventSubMessage) : Input
        data class Closed(val connectionId: Long, val error: Throwable?) : Input
        data class KeepaliveExpired(val connectionId: Long) : Input
        data class SubscriptionResult(val connectionId: Long, val success: Boolean) : Input
        data class Retry(val token: Long) : Input
    }

    private companion object {
        const val TAG = "ChatRepository"
        const val KEEPALIVE_TIMEOUT_SECONDS = 30
        const val KEEPALIVE_GRACE_MILLIS = 5_000L
        const val INITIAL_BACKOFF_MILLIS = 1_000L
        const val MAX_BACKOFF_MILLIS = 30_000L
        const val MAX_BACKOFF_SHIFT = 5
    }
}
