package com.example.lobchat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultLauncher
import com.google.android.material.snackbar.Snackbar

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view) // 确保这个布局文件存在

        // 初始化 WebView
        webView = findViewById(R.id.webView)
        setupWebView()

        // 获取传递的 URL
        val url = intent.getStringExtra("URL") // 确保这个 key 和 MainActivity 一致
        if (!url.isNullOrEmpty()) {
            webView.loadUrl(url)
        } else {
            showSnackbar("No URL provided")
        }
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                showSnackbar("Network error, please try again later.")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 注入 JavaScript 来禁用 PWA 安装提示
                injectJavaScript(view)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                this@WebViewActivity.filePathCallback = filePathCallback
                launchFileChooser()
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.e("WebViewConsole", "JavaScript Error: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }
    }

    private fun injectJavaScript(view: WebView?) {
        view?.evaluateJavascript(
            """
            window.addEventListener('beforeinstallprompt', function(e) {
                e.preventDefault();
                console.log('Install prompt blocked by WebView');
            });
            """.trimIndent(), null
        )
    }

    private fun launchFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        fileChooserLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
