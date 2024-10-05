package com.example.lobchat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 用于替代 startActivityForResult 的新文件选择器启动器
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查并请求读取权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        // 设置状态栏为透明，使系统自动使用默认的颜色
        window.statusBarColor = resources.getColor(android.R.color.transparent, theme)

        // 初始化 WebView
        webView = findViewById(R.id.webview)

        // 启用 JavaScript
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
            }
        }

        // 设置 WebChromeClient 以处理文件选择
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // 处理新的文件选择请求
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"

                // 使用新的 ActivityResultLauncher 启动文件选择器
                fileChooserLauncher.launch(Intent.createChooser(intent, "Select Picture"))
                return true
            }
        }

        // 加载您的网站
        webView.loadUrl("https://chat.dolingou.com") // 替换为您的 URL
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Read permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Read permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
