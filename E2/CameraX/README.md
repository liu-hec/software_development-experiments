

# CameraDemo

## 一 目的

   完成一个基于cameraX的相机app



## 二 实现过程

​     明确gradle和cameraX的依赖适配性，采用传统的xml形式完成ui设计，注意兼容性。以gradle9.4.1为例

  1**.前提准备**



  **开启视图绑定**

​    **ViewBinding（视图绑定）** 是 Android 专门给 **XML 布局** 提供的工具，作用是 **替代繁琐的 `findViewById`**，安全、简洁地获取界面控件（按钮、文本、布局等）。同时创建activity_main.xml主布局页面。



   **配置权限**

```
<uses-feature android:name="android.hardware.camera.any" />  <!--前置或者后置-->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```





2. **基本布局**

​    在activity_main中创建两个button，完成ui设计。

​    

​       创建基本的页面图，和基本的代码结构。绑定界面布局，检查权限和处理button点击事件，启动异步线程（为analysis的处理线程）

```
companion object {
    private const val TAG = "CameraXApp"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
} //存放固定不变的配置：权限列表、照片名字格式、日志标记
```



3.  **完成权限请求方法**

<img width="1143" height="777" alt="image" src="https://github.com/user-attachments/assets/751ac7e7-a4a8-4c55-b5e1-218d1d97b782" />



4.**核心流程**



   完善startCamera方法，这是核心方法，融合其他的具体实现例如takephoto方法。

  获取 CameraProvider（相机总管）：`ProcessCameraProvider.getInstance(this)`

   等相机初始化完成后：

- ​        创建 **Preview** 用例
- ​        创建 **ImageCapture** 用例（拍照）
- ​        创建 **VideoCapture** 用例（录像）
- ​        创建 **ImageAnalysis** 用例（分析亮度）

​      核心流程：获取相机管理器 → 配置预览 → 选择摄像头 → 绑定 → 相机开始工作



**Preview方法**

```
val preview = Preview.Builder().build()
it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider) //将预览页面放在屏幕
...
..
cameraProvider.unbindAll()
cameraProvider.bindToLifecycle(...) //先清掉旧相机，再把所有功能绑定到生命周期上
```



**重写takephoto方法**

​    流程：

1. 确认 `imageCapture` 已准备好
2. 生成文件名，按时间戳命名
3. 选择保存目录（App 私有目录 `getExternalFilesDir("CameraX")`）
4. 构建输出文件配置：`ImageCapture.OutputFileOptions.Builder(photoFile).build()`
5. 拍照回调，拍成功弹出 Toast 提示，同时打印日志



 **photo analysis方法**

```
val imageAnalyzer = ImageAnalysis.Builder().build().also {
    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
        Log.d(TAG, "Average luminosity: $luma")
    })
} //每一帧画面都拿去算亮度，然后打印日志
```

<img width="1132" height="600" alt="image" src="https://github.com/user-attachments/assets/f0eaee93-ac47-41b7-8e22-402769640ab0" />



**video方法**

```
recording = videoCapture.output.prepareRecording(...)
    .start(...) //录制
```

<img width="1151" height="806" alt="image" src="https://github.com/user-attachments/assets/e2e807a6-e951-4d41-a2e9-980375de9519" />

 

  存放在MediaStore中，可以发现vedio录制成功

<img width="485" height="269" alt="image" src="https://github.com/user-attachments/assets/520b05e9-e24a-498b-9ef9-37b9fce0e90d" />





```
cameraProvider
    .bindToLifecycle(this, cameraSelector,
        preview,
        imageCapture,
        videoCapture,
        imageAnalyzer
    )  一次绑定所有的use cases
```

​            这样我们可以边录制，边拍照和分析。



   这样完成了preview，photo,vedio和普通的analysis的混合支持。将相机的绑定到Activity的生命周期上。这样我们无需重写二外的生命周期函数，附属于Activity。



## 三 总结

​        通过本实验，能够基于 CameraX流程和AI辅助 完成一个功能完整的相机 App，实现多功能混合使用（预览、拍照、录像、图像分析），并了解相机生命周期管理的绑定、权限控制和文件保存的核心技能。





