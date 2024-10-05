package com.example.lobchat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.EditText
import android.widget.Button
import android.view.View
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    private lateinit var urlInput: EditText
    private lateinit var loadUrlButton: Button

    companion object {
        const val SHARED_PREFS_NAME = "lobchat_prefs"
        const val KEY_SAVED_URL = "saved_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图组件
        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        loadUrlButton = findViewById(R.id.loadUrlButton)

        // 从 SharedPreferences 加载上次保存的 URL
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(KEY_SAVED_URL, "")
        if (!savedUrl.isNullOrEmpty()) {
            urlInput.setText(savedUrl)
        }

        // 设置状态栏为透明，使系统自动使用默认的颜色
        window.statusBarColor = resources.getColor(android.R.color.transparent, theme)

        // 初始化 WebView 设置
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // 初始化文件选择器启动器
        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val resultUri = result.data?.data
                if (filePathCallback != null) {
                    filePathCallback?.onReceiveValue(resultUri?.let { arrayOf(it) })
                    filePathCallback = null
                }
            } else {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }

        // 确保链接在 WebView 内部打开，而不是在默认浏览器中打开
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Toast.makeText(this@MainActivity, "Network error, please try again later.", Toast.LENGTH_SHORT).show()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 注入 JavaScript 来禁用 PWA 安装提示
                view?.evaluateJavascript(
                    """
                    window.addEventListener('beforeinstallprompt', function(e) {
                        e.preventDefault();
                        console.log('Install prompt blocked by WebView');
                    });
                    """.trimIndent(), null
                )
                // 隐藏输入框和按钮
                urlInput.visibility = View.GONE
                loadUrlButton.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        }

        // 设置 WebChromeClient 以处理文件选择和 JavaScript 错误
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"

                // 使用新的 ActivityResultLauncher 启动文件选择器
                fileChooserLauncher.launch(Intent.createChooser(intent, "Select Picture"))
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.e("WebViewConsole", "JavaScript Error: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }

        // 点击按钮后加载用户输入的 URL
        loadUrlButton.setOnClickListener {
            var url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                // 确保 URL 包含 http 或 https
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }

                // 保存用户输入的 URL 到 SharedPreferences
                with(sharedPreferences.edit()) {
                    putString(KEY_SAVED_URL, url)
                    apply()
                }

                // 加载用户输入的 URL
                webView.loadUrl(url)
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 此时不再需要权限检查方法
}
