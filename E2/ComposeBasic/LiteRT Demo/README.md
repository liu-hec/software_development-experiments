

# LiteRT Camera Demo

## 一 实验内容：

<img width="1134" height="634" alt="image" src="https://github.com/user-attachments/assets/1a4a61aa-9f59-4607-adf8-a47e2ea4b451" />



​    使用compose构建完成上面的图片。

## 二、实验原理

### 1. Compose 状态驱动 UI

该程序采用 Jetpack Compose 的声明式 UI 方式，界面内容由状态变量决定。
 因此代码中使用了多个状态变量：

- `modelName`：模型名称
- `result`：识别结果
- `confidence`：置信度
- `time`：耗时
- `imageUri`：图片地址

当这些变量发生变化时，Compose 会自动重新组合界面，从而更新显示内容。如果使用普通变量，不会捕捉其变化并显示

### 2. 图片选择功能

使用了：

```
rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
)
```

  这个组件用于启动系统文件选择器。
 当用户选择图片后，返回一个 `Uri`，并赋值给 `imageUri`，随后通过 Coil 的：

```
rememberAsyncImagePainter(imageUri)
```

将图片显示在界面中。

### 3. 页面布局结构

整个界面由 `Scaffold` 作为基础布局容器构成，包含：

- 顶部导航栏 `TopAppBar`
- 内容区域 `Column`
- 图片显示框 `Box`
- 信息展示卡片 `Card`
- 两行功能按钮 `Row`



## 三、核心代码分析

### 1. 主活动入口

`  MainActivity` 中通过 `setContent {}` 加载 Compose 界面，并启用了沉浸式布局：

```
enableEdgeToEdge()
```

  同时设置状态栏图标颜色，保证顶部区域显示效果更美观。

### 2. 图片预览区域

  图片区域采用 `Box` 容器：

- 当 `imageUri == null` 时，显示相机图标和文字 “Camera Preview”
- 当用户导入图片后，改为显示所选图片

这体现了 Compose 中“条件渲染”的特点。

### 3. 结果显示卡片

  `Card` 用于显示当前识别信息，包括：

- 模型名称
- 识别结果
- 置信度
- 耗时

​    这些内容目前是模拟数据。Icon和Text嵌套在Column中，使其垂直分布。

<img width="792" height="245" alt="image" src="https://github.com/user-attachments/assets/3257b5c6-cc89-4a67-9092-c5ecada2d442" />

### 4. 按钮交互逻辑

程序中设置了四个功能按钮，其中button左侧嵌入Icon。点击按钮模拟显示内容的变化

- **拍照识别**：点击后修改结果、置信度和耗时
- **相册导入**：打开系统相册选择图片
- **切换模型**：在 MobileNet 和 ResNet 间切换
- **清空结果**：清除页面上的结果信息

这些按钮主要用于验证界面状态更新和交互响应。



<img width="1144" height="661" alt="image" src="https://github.com/user-attachments/assets/b1516ec1-acf3-41a9-a593-e58595678c54" />



##  四 总结

​       拆解页面布局，从骨架分析到具体的固件选择，借以AI辅助完成，更加体会compose的便捷。
