package com.streamhelper.core.oauth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen in-app OAuth login (implicit flow). Loads [OAuthRequest.authorizeUrl] in a WebView with
 * JavaScript, DOM storage and third-party cookies enabled; intercepts any navigation to
 * [OAuthRequest.redirectUri] without loading it and reports exactly one [OAuthResult] via [onResult]:
 * [OAuthResult.Success]/[OAuthResult.Failed] from the redirect, [OAuthResult.Cancelled] on system Back.
 *
 * Host it as an overlay above the caller's screen and remove it once [onResult] fires.
 * No deep links or App Links are involved.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OAuthWebView(
    request: OAuthRequest,
    onResult: (OAuthResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnResult by rememberUpdatedState(onResult)
    // Report once only: the redirect can be seen by both callbacks below, and Back may race it.
    val reported = remember { AtomicBoolean(false) }
    val report: (OAuthResult) -> Unit = { result ->
        if (reported.compareAndSet(false, true)) currentOnResult(result)
    }

    BackHandler { report(OAuthResult.Cancelled) }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, webRequest: WebResourceRequest): Boolean {
                        val result = OAuthRedirect.parse(webRequest.url.toString(), request.redirectUri) ?: return false
                        report(result)
                        return true
                    }

                    // Fallback: shouldOverrideUrlLoading isn't called for some navigations (e.g. POST).
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        val result = OAuthRedirect.parse(url, request.redirectUri)
                        if (result == null) {
                            super.onPageStarted(view, url, favicon)
                            return
                        }
                        view.stopLoading()
                        report(result)
                    }
                }
                loadUrl(request.authorizeUrl)
            }
        },
        onRelease = { it.destroy() },
    )
}
