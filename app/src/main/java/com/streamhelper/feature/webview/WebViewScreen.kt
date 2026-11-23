package com.streamhelper.feature.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** RN: screens/WebView — generic in-app browser for a given URI. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(uri: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        factory = { context ->
            WebView(context).apply {
                // AndroidView gives the view WRAP_CONTENT params; WebView then forces the page's layout
                // height to 0 (100%/vh resolve to 0), so full-height pages like Twitch login render blank.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = WebViewClient()
                loadUrl(uri)
            }
        },
    )
}
