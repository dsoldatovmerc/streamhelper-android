package com.streamhelper.feature.chat.data

import com.streamhelper.core.network.Twitch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ChatModerationModule {
    @Provides
    fun chatModerationApi(@Twitch retrofit: Retrofit): ChatModerationApi =
        retrofit.create(ChatModerationApi::class.java)
}
