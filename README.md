# 一个[LobeChat](https://lobechat.com/)的Android客户端

## 支持功能

- 使用自定义LobeChat服务地址
- 支持上传图片

## 请求权限

```bash
    <uses-permission android:name="android.permission.INTERNET" />
```

目前有一处权限请求：

1. 使用网络请求，用于打开LobeChat服务网页；

## 使用方法

下载位置在Github Releases里。我还不怎么会用Github Action和Release，完全是新手入门。

https://github.com/moreoronce/LobeChat-Android/releases

目前已经测试Android 14（API 34）和Android 12是使用没有问题，其他版本的兼容都还没测试。

应用的使用速度大部分都看目标网站的速度，如果目标网站的速度不够快，那么应用也会白屏等待。

## 关于后门

对不起我现在水平还没到可以写后门的地步，即便是让我问AI，我都不知道怎么问。所有的代码都在Github上，也可以自己进行编译或者修改。

# 🤗 下一步

目前需要解决的问题还很多：
- [ ]  在WebView增加刷新与返回首页功能。
- [ ]  优化缓存与WebView的使用体验。
- [ ]  页面美化，与LobeChat保持一致。
- [ ]  增加页面加载进度条，优化页面加载体验。
- [X]  安装包更新优化，目前需要先卸载再安装。
- [x]  增加自动记录自定义URL地址功能，避免每次需要手动输入
- [X]  现在输入任意URL之后就会通过WebView打开，目前考虑要不要增加判断只允许使用特定服务。
- [X]  解决Google账号无法登录问题
- [X]  解决安装包的证书问题。



