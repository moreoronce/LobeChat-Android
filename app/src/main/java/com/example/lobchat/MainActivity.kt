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

        // 点击按钮后加载用户输入的 URL
        loadUrlButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            val urlPattern = Regex("^(?:(http|https):\\/\\/)?((?:[\\w-]+\\.)+[a-z0-9]+)((?:\\/[^/?#]*)+)?(\\?[^#]+)?(#.+)?$")

            if (url.isNotEmpty()) {
                // 确保 URL 包含 http 或 https
                val formattedUrl = if (!urlPattern.matches(url)) {
                    // 如果 URL 格式不正确，则提示用户
                    Toast.makeText(this, "请输入正确的网站地址", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener // 结束函数
                } else {
                    // 确保 URL 包含 http 或 https
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        "https://$url"
                    } else {
                        url
                    }
                }

                // 保存用户输入的 URL 到 SharedPreferences
                with(sharedPreferences.edit()) {
                    putString(KEY_SAVED_URL, formattedUrl)
                    apply()
                }

                // 启动 WebViewActivity
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra("URL", formattedUrl)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
