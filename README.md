## 支持功能

- 使用自定义Lobe Chat服务地址
- 支持上传图片
- 支持上传音频

## 请求权限

```bash
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>
```

目前有四处权限请求：

1. 使用网络请求，用于打开Lobe Chat服务网页；
2. 读取图片请求；

## 使用方法

下载位置在Github Action里，取最近一份成功编译的文件即可。我还不怎么会用Github Action和Release，完全是新手入门。

```bash
https://github.com/moreoronce/lobe-chat-android/actions/runs/11191700508
```

目前已经测试Android 14（API 34）和Android 12是使用没有问题，其他版本的兼容都还没测试。

## 关于后门

对不起我现在水平还没到可以写后门的地步，即便是让我问AI，我都不知道怎么问。所有的代码都在Github上，也可以自己进行编译或者修改。

## 下一步

目前需要解决的问题还很多：

- [ ]  现在输入任意URL之后就会通过WebView打开，目前考虑要不要增加判断只允许使用特定服务。
- [ ]  在WebView增加刷新与返回首页功能
- [ ]  优化缓存与WebView的使用体验
- [ ]  解决安装包的证书问题
- [ ]  页面美化，与Lobe Chat保持一致
- [ ]  增加自动记录自定义URL地址功能，避免每次需要手动输入
