package com.streamhelper.feature.chat.data

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * OkHttp WebSocket wrapper for Twitch EventSub (RN `WebSocketStore`).
 *
 * [connect] returns a cold flow: collecting it opens one socket and emits its parsed frames;
 * cancelling the collector closes the socket. The flow completes when the server closes the
 * socket and throws on a socket failure. Unparseable frames are logged and skipped.
 * Connection bookkeeping (no-op while connected, reconnects, migration) lives in [ChatRepository].
 */
class EventSubSocket @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    fun connect(url: String): Flow<EventSubMessage> = callbackFlow {
        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = EventSubParser.parse(json, text)
                if (message == null) {
                    Log.w(TAG, "Skipping unparseable EventSub frame")
                } else {
                    trySend(message)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "EventSub closing: $code $reason")
                webSocket.close(NORMAL_CLOSURE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                channel.close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                channel.close(t)
            }
        }
        val socket = client.newWebSocket(Request.Builder().url(url).build(), listener)
        awaitClose {
            if (!socket.close(NORMAL_CLOSURE, null)) socket.cancel()
        }
    }.buffer(Channel.UNLIMITED) // never drop chat frames while the consumer is busy

    private companion object {
        const val TAG = "EventSubSocket"
        const val NORMAL_CLOSURE = 1000
    }
}
