package com.example.lobchat

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar


class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var isFinalUrlLoaded = false
    private var isLobeChatVerified = false
    private lateinit var refreshButton: FloatingActionButton

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        refreshButton = findViewById(R.id.refreshButton)
        // 初始化 WebView
        webView = findViewById(R.id.webView)

        setupWebView()

        // 刷新按钮的点击事件
        refreshButton.bringToFront()
        refreshButton.setOnClickListener {
            Log.i("WebViewActivity", "Refresh button clicked")
            webView.reload()
        }

        // 设置拖动事件监听器
        refreshButton.setOnTouchListener(DraggableTouchListener())

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

    // 处理返回键
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // Use default caching strategy

        // Other optimizations
        webSettings.databaseEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Pixel 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                isFinalUrlLoaded = false
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
                            isLobeChatVerified = true
                            Toast.makeText(this@WebViewActivity, "LobeChat validation passed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@WebViewActivity, "The webpage does not contain the required LobeChat text in the header.", Toast.LENGTH_LONG).show()
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

    private inner class DraggableTouchListener : View.OnTouchListener {

        private var dX = 0f
        private var dY = 0f
        private var isClick = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    isClick = true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    isClick = false
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        // 处理点击事件
                        Log.i("WebViewActivity", "Refresh button clicked")
                        webView.reload()
                    }
                }
            }
            return true
        }
    }
}
