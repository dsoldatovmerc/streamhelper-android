package com.streamhelper.feature.user

import com.streamhelper.core.network.Twitch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    @Provides
    fun userApi(@Twitch retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)
}
