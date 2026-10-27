package com.taku.funpaymobile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var themeButton: Button
    private var darkMode = true
    private val blue = Color.rgb(67, 164, 220)
    private val dark = Color.rgb(39, 42, 47)
    private val darker = Color.rgb(29, 31, 35)
    private val light = Color.rgb(245, 247, 249)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = darker
        window.navigationBarColor = darker
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(5), dp(6), dp(5))
            setBackgroundColor(darker)
        }
        fun button(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(dark)
            setOnClickListener { action() }
        }
        bar.addView(button("‹") {
            if (web.canGoBack()) web.goBack() else web.loadUrl("https://funpay.com/")
        }, LinearLayout.LayoutParams(dp(48), dp(44)))
        bar.addView(button("Домой") { web.loadUrl("https://funpay.com/") },
            LinearLayout.LayoutParams(0, dp(44), 1f))
        bar.addView(button("Обновить") { web.reload() },
            LinearLayout.LayoutParams(0, dp(44), 1f))
        themeButton = button("Светлая") {
            darkMode = !darkMode
            updateTheme()
        }
        bar.addView(themeButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(bar, LinearLayout.LayoutParams(-1, dp(54)))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(blue)
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        web = WebView(this).apply {
            setBackgroundColor(dark)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    val host = uri.host.orEmpty().lowercase()
                    if (host == "funpay.com" || host.endsWith(".funpay.com") || uri.scheme == "about") return false
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                override fun onPageFinished(view: WebView, url: String) { updateTheme() }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://funpay.com/")
        updateTheme()
    }

    private fun updateTheme() {
        if (!::web.isInitialized) return
        web.setBackgroundColor(if (darkMode) dark else light)
        themeButton.text = if (darkMode) "Светлая" else "Тёмная"
        val css = if (darkMode) """
            html, body { background:#272a2f !important; color:#e7e9ec !important; }
            .container, .wrapper, .content, .form-control, .modal-content, .chat, .chat-message,
            .offer, .offer-list, .list-group-item, .alert, .dropdown-menu, .table, .nav-tabs, .nav-pills,
            .panel, .panel-heading, .panel-body, .well, .popover, .tooltip-inner {
              background-color:#272a2f !important; color:#e7e9ec !important; border-color:#454a52 !important;
            }
            input, textarea, select { background:#202328 !important; color:#f1f3f5 !important; border-color:#505761 !important; }
            a { color:#62b9ec !important; }
            .navbar, .navbar-default { background-color:#1d1f23 !important; color:#e7e9ec !important; border-color:#454a52 !important; }
            .text-muted, .help-block { color:#aeb5be !important; }
        """.trimIndent() else """
            html, body { background:#f5f7f9 !important; color:#222 !important; }
            .container, .wrapper, .content, .form-control, .modal-content, .chat, .chat-message,
            .offer, .offer-list, .list-group-item, .alert, .dropdown-menu, .table, .nav-tabs, .nav-pills,
            .panel, .panel-heading, .panel-body, .well { background-color:#ffffff !important; color:#222 !important; }
            input, textarea, select { background:#ffffff !important; color:#222 !important; }
            a { color:#167eb5 !important; }
        """.trimIndent()
        val js = "(function(){var o=document.getElementById('funpay-mobile-theme');if(o)o.remove();var s=document.createElement('style');s.id='funpay-mobile-theme';s.textContent=${org.json.JSONObject.quote(css)};document.head.appendChild(s);})();"
        web.evaluateJavascript(js, null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::web.isInitialized) web.saveState(outState)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
