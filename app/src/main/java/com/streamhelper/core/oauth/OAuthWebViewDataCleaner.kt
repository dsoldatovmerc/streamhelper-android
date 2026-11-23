package com.streamhelper.core.oauth

import android.webkit.CookieManager
import android.webkit.WebStorage
import com.streamhelper.core.coroutines.MainDispatcher
import com.streamhelper.core.session.LogoutHook
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Logout hook: wipes WebView cookies and web storage so the next [OAuthWebView] login shows the
 * provider's sign-in page instead of silently reusing the previous account's session.
 */
class OAuthWebViewDataCleaner @Inject constructor(
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : LogoutHook {
    override suspend fun onLoggedOut() = withContext(mainDispatcher) {
        WebStorage.getInstance().deleteAllData()
        val cookies = CookieManager.getInstance()
        suspendCancellableCoroutine { cont -> cookies.removeAllCookies { cont.resume(Unit) } }
        cookies.flush()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OAuthModule {
    @Binds
    @IntoSet
    abstract fun bindWebViewDataCleaner(impl: OAuthWebViewDataCleaner): LogoutHook
}
