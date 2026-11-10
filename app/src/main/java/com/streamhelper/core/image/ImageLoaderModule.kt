package com.streamhelper.core.image

import android.content.Context
import android.os.Build
import coil3.Extras
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.MovieDrawable
import coil3.gif.repeatCount
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

/**
 * The app-wide Coil [ImageLoader] (emotes, avatars), installed as Coil's singleton by
 * `StreamHelperApplication`, so `AsyncImage` uses it without passing it around.
 *
 * Memory and disk caches; network over the shared [OkHttpClient]. Animated WebP/GIF decode with
 * [AnimatedImageDecoder] on API 28+ and loop forever; on API 26–27 they decode as a static first frame.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    @Provides
    @Singleton
    fun imageLoader(@ApplicationContext context: Context, okHttpClient: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .apply { extras[Extras.Key.repeatCount] = MovieDrawable.REPEAT_INFINITE }
            .build()

    private const val MEMORY_CACHE_PERCENT = 0.25
    private const val DISK_CACHE_DIR = "image_cache"
    private const val DISK_CACHE_BYTES = 100L * 1024 * 1024
}
