#              基于 TensorFlow Lite 的

#             Android 花卉识别应用开发





## 一、实验内容

   本实验基于 Google 官方 Codelab 教程，利用 TensorFlow Lite框架，在 Android 设备上构建一个能够实时识别花卉种类的智能相机应用。





## 二、实验过程

###  2.1 实验准备

  android studio 和对应的model文件FlowerModel.tflite





### 2.2 实验ui设计

​    界面布局，含 PreviewView + RecyclerView 在主xml中实现

<img width="756" height="603" alt="image" src="https://github.com/user-attachments/assets/a541e4ad-dedd-463e-be9e-9b5e04ebffc8" />

​        RecyclerView 单条识别结果的 Item 布局（Data Binding），将recognition数据和recognition_item的UI视图绑定。



​       

### 2.3 具体流程

2. 3.1 CameraX,预览和进行图片分析。

| ***\*用例类\**** | ***\*作用\****                       | ***\*本项目中的使用\****                               |
| ---------------- | ------------------------------------ | ------------------------------------------------------ |
| Preview          | 将摄像头画面渲染到 PreviewView 控件  | 在 activity_main.xml 的 PreviewView 上实时显示取景画面 |
| ImageCapture     | 拍照并保存到文件                     | 本项目未使用（仅识别，不保存）                         |
| ImageAnalysis    | 逐帧将 ImageProxy 传给 Analyzer 回调 | 核心用例：每帧调用 ImageAnalyzer.analyze() 执行推理    |

​     CameraX 的重要特性：它是生命周期感知的（Lifecycle-Aware），通过 bindToLifecycle() 绑定到 Activity/Fragment 后，相机会随生命周期自动启停，开发者无需在 onResume/onPause 中手动管理。



  **数据流通具体流程：**

​     CameraX ImageAnalyzer → 推理结果 List<Recognition> → ViewModel.recognitionList.postValue() → LiveData 通知 → RecyclerView 更新界面。

​     

#### 2.3.2 核心类ImageAnalyzer

   （1）  回调函数，将分析器的数据传回ViewModel, recogViewModel.updateData(items)更新数据

```
typealias RecognitionListener = (recognition: List<Recognition>) -> Unit
一个函数：
接收 List<Recognition>
返回 Unit（无返回值）
```

​    RecognitionListener  将分析器的结果传出去更新UI。

  

（2）模型初始化

```
        private val flowerModel = FlowerModel.newInstance(ctx)
```

​     这里初始化了一个名为 `FlowerModel` 的高科技工具，它就是由 TensorFlow Lite 自动生成的**花朵识别模型**（比如识别玫瑰、向日葵、郁金香等）。

  

​      接下来根据Image来analyze，相机的原始画面 `imageProxy` 是 `YUV_420_888` 这种复杂的硬件格式，因此进行二进制化toBitmap

```
val tfImage = TensorImage.fromBitmap(toBitmap(imageProxy))
```

（3）计算并排序地显示

  model.process计算，返回一个**所有花朵概率的列表**（比如：玫瑰 80%，向日葵 15%，牡丹 5%），按照概率**从大到小排序**。

```
  val outputs = flowerModel.process(tfImage)
                .probabilityAsCategoryList.apply {
                    sortByDescending { it.score } // Sort with highest confidence first
                }.take(MAX_RESULT_DISPLAY) // take the top results

```



 （4）  构建结果数据对象

```
 for (output in outputs) {
                items.add(Recognition(output.label, output.score))
            }
```





（5）启动GPU加速

```
private val flowerModel: FlowerModel by lazy {
            val compatList = CompatibilityList()
            val options = if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(TAG, "This device is GPU Compatible")
                TFLiteModel.Options.Builder().setDevice(TFLiteModel.Device.GPU).build()
            } else {
                Log.d(TAG, "This device is GPU Incompatible")
                TFLiteModel.Options.Builder().setNumThreads(4).build()
            }

            FlowerModel.newInstance(ctx, options)
        }
```

​     先检查兼容性，如果适配使用GPU加速，否则启动cpu多线程加速计算。懒加载，第一次被使用，才返回模型实例。



### 2.4 调试

  虚拟机情况

<img width="671" height="854" alt="image" src="https://github.com/user-attachments/assets/f3c54714-f9c5-4ace-930b-478d392faa67" />

​    导出start的apk文件，在真机运行.

rose

<img width="449" height="899" alt="image" src="https://github.com/user-attachments/assets/18394c87-06a0-4fec-a4a1-2ccafdb578d9" />

向日葵

<img width="440" height="804" alt="image" src="https://github.com/user-attachments/assets/d1700bfe-4380-4f63-8158-d009c63d1c49" />

郁金香

<img width="422" height="845" alt="image" src="https://github.com/user-attachments/assets/bf055727-8a77-4c5c-9f5f-35e669111f12" />



蒲公英 

<img width="433" height="882" alt="image" src="https://github.com/user-attachments/assets/5f6d8e30-70a7-4329-94fd-636cdeeb9ef5" />

  雏菊daisy

<img width="424" height="899" alt="image" src="https://github.com/user-attachments/assets/e801f5b0-e8ac-4185-9085-5fc0f1aeacbf" />

 
