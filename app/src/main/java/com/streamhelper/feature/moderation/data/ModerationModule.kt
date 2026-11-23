package com.streamhelper.feature.moderation.data

import com.streamhelper.core.network.Twitch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ModerationModule {
    @Provides
    fun moderationApi(@Twitch retrofit: Retrofit): ModerationApi = retrofit.create(ModerationApi::class.java)
}
