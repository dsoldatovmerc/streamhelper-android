package com.streamhelper.feature.chat.data

import com.streamhelper.core.network.Twitch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.DELETE
import retrofit2.http.Query

/** Helix chat moderation endpoints that change the message list (relative to the Helix base URL). */
interface ChatModerationApi {
    /** Deletes one chat message. Twitch answers 204 No Content. */
    @DELETE("moderation/chat")
    suspend fun deleteMessage(
        @Query("broadcaster_id") broadcasterId: String,
        @Query("moderator_id") moderatorId: String,
        @Query("message_id") messageId: String,
    )
}

@Module
@InstallIn(SingletonComponent::class)
object ChatModerationApiModule {
    @Provides
    fun chatModerationApi(@Twitch retrofit: Retrofit): ChatModerationApi =
        retrofit.create(ChatModerationApi::class.java)
}
