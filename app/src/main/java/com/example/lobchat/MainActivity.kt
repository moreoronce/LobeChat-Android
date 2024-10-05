package com.example.lobchat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.widget.EditText
import android.widget.Button
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.provider.Settings
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInputLayout: ConstraintLayout
    private lateinit var urlInput: EditText
    private lateinit var loadUrlButton: Button
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图组件
        webView = findViewById(R.id.webView)
        urlInputLayout = findViewById(R.id.urlInputLayout)
        urlInput = findViewById(R.id.urlInput)
        loadUrlButton = findViewById(R.id.loadUrlButton)

        // 检查并请求存储权限
        if (!hasStoragePermission()) {
            requestStoragePermission()
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

                // 加载用户输入的 URL
                webView.loadUrl(url)

                // 切换视图：隐藏 URL 输入部分并显示 WebView
                urlInputLayout.visibility = View.GONE
                webView.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 方法：检查是否具有存储权限
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 方法：请求存储权限
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                showPermissionRationale(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 1)
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        }
    }

    // 方法：显示权限请求解释
    private fun showPermissionRationale(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires access to your storage to select images. Please grant the permission.")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Read permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Read permission denied", Toast.LENGTH_SHORT).show()
                // 引导用户前往设置手动开启权限
                AlertDialog.Builder(this)
                    .setTitle("Permission Denied")
                    .setMessage("Storage permission is required for this app. Please allow it from settings.")
                    .setPositiveButton("Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
}
