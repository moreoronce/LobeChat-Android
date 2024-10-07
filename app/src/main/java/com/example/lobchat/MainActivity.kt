package com.example.lobchat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var loadUrlButton: Button

    companion object {
        const val SHARED_PREFS_NAME = "lobchat_prefs"
        const val KEY_SAVED_URL = "saved_url"
    }


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图组件
        urlInput = findViewById(R.id.urlInput)
        loadUrlButton = findViewById(R.id.loadUrlButton)


        // 从 SharedPreferences 加载上次保存的 URL
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(KEY_SAVED_URL, "")
        if (!savedUrl.isNullOrEmpty()) {
            urlInput.setText(savedUrl)
        }

        // 点击按钮后加载用户输入的 URL
        loadUrlButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            val urlPattern = Regex("^(?:(http|https):\\/\\/)?((?:[\\w-]+\\.)+[a-z0-9]+)((?:\\/[^/?#]*)+)?(\\?[^#]+)?(#.+)?$")

            if (url.isNotEmpty()) {
                // 确保 URL 包含 http 或 https
                val formattedUrl = if (!urlPattern.matches(url)) {
                    Toast.makeText(this, "请输入正确的网站地址", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
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
