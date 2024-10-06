package com.example.lobchat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var isFinalUrlLoaded = false
    // 用于标记是否通过 LobeChat 验证
    private var isLobeChatVerified = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        // 初始化 WebView
        webView = findViewById(R.id.webView)
        setupWebView()

        // 初始化文件选择器启动器
        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleFileChooserResult(result)
        }

        // 获取传递的 URL
        val url = intent.getStringExtra("URL")
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
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // 当 URL 变化时重置 isFinalUrlLoaded 标志
                isFinalUrlLoaded = false
                // 返回 false 表示允许 WebView 处理请求
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (!isLobeChatVerified) {
                    view?.evaluateJavascript(
                        """
                        (function() {
                            var metaTags = document.getElementsByTagName('head')[0].innerHTML;
                            return metaTags.includes('lobechat');
                        })();
                        """.trimIndent()
                    ) { result ->
                        if (result == "true") {
                            // 验证通过，不再继续验证
                            isLobeChatVerified = true
                            Toast.makeText(this@WebViewActivity, "LobeChat validation passed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@WebViewActivity, "The webpage does not contain the required LobeChat text in the header.", Toast.LENGTH_LONG).show()
                            // 如果验证失败，返回 MainActivity
                            val intent = Intent(this@WebViewActivity, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
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

    private fun launchFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        fileChooserLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    private fun handleFileChooserResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            val resultUri = result.data?.data
            filePathCallback?.onReceiveValue(resultUri?.let { arrayOf(it) })
            filePathCallback = null
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
