package com.streamhelper.feature.chat.data

import com.streamhelper.core.network.Twitch
import com.streamhelper.core.session.LogoutHook
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ChatApiModule {
    @Provides
    fun eventSubApi(@Twitch retrofit: Retrofit): EventSubApi = retrofit.create(EventSubApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatHooksModule {
    @Binds
    @IntoSet
    abstract fun bindChatLogoutHook(impl: ChatRepository): LogoutHook
}
