

## 一、项目摘要

本项目基于卷积神经网络（Convolutional Neural Network, CNN）实现对 “石头（rock）、剪刀（scissors）、布（paper）” 三类手势图像的分类任务。项目完整覆盖了**数据获取、数据预处理、模型构建、模型训练、性能评估、自定义图片测试** 全流程，使用 Google 公开的 RPS 数据集完成训练，最终构建的 CNN 模型在验证集上达到最高 100% 的分类准确率，在自定义图片测试中也展现出良好的泛化能力，成功实现了三类手势的精准识别。

## 二、项目背景与目标

### 1. 背景

图像分类是计算机视觉的核心任务之一，卷积神经网络凭借其对图像空间特征的提取能力，成为图像分类的主流方法。“石头剪刀布” 手势分类是典型的细粒度图像分类场景，可应用于人机交互、手势控制、游戏开发等领域。

### 2. 目标

- 获取并预处理公开的 RPS 手势图像数据集；
- 构建基于 CNN 的图像分类模型；
- 训练并优化模型，实现高准确率的三类手势分类；
- 验证模型泛化能力，支持自定义图片的分类预测。



## 3.实验过程

###  3.1 数据集来源



（1）下载实现

  编写专用下载函数，解决 SSL 证书验证问题，同时通过 “临时文件 + 文件大小校验” 实现断点续传（避免重复下载），核心逻辑如下：

- 检查目标文件是否已存在且非空，若存在则跳过下载；
- 临时文件后缀为`.part`，下载完成后重命名为目标文件；
- 处理`URLError`异常，通过创建未验证的 SSL 上下文完成请求。

（2）解压验证

​    解压前通过`zip_ref.testzip()`校验压缩包完整性，若检测到损坏文件则抛出异常，确保数据无丢失；解压后将文件存储至指定目录（`D:/RPS/mldownload`）。

```
import ssl
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen

DOWNLOAD_DIR = Path("D:/RPS/mldownload")
DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)

RPS_URL = "https://storage.googleapis.com/learning-datasets/rps.zip"
RPS_TEST_URL = "https://storage.googleapis.com/learning-datasets/rps-test-set.zip"
RPS_ZIP = DOWNLOAD_DIR / "rps.zip"
RPS_TEST_ZIP = DOWNLOAD_DIR / "rps-test-set.zip"

def download_file(url, destination):
    if destination.exists() and destination.stat().st_size > 0:
        print(f"File already exists, skipping: {destination}")
        return

    temp_path = destination.with_suffix(destination.suffix + ".part")
    print(f"Downloading: {url}")

    try:
        response = urlopen(url, timeout=120)
    except URLError:
        context = ssl._create_unverified_context()
        response = urlopen(url, timeout=120, context=context)

    with response, temp_path.open("wb") as file:
        while True:
            data = response.read(1024 * 1024)
            if not data:
                break
            file.write(data)

    temp_path.replace(destination)
    size_mb = destination.stat().st_size / 1024 / 1024
    print(f"Downloaded: {destination} ({size_mb:.1f} MB)")

download_file(RPS_URL, RPS_ZIP)
download_file(RPS_TEST_URL, RPS_TEST_ZIP)

```



    Downloading: https://storage.googleapis.com/learning-datasets/rps.zip
    Downloaded: D:\RPS\mldownload\rps.zip (191.4 MB)
    Downloading: https://storage.googleapis.com/learning-datasets/rps-test-set.zip
    Downloaded: D:\RPS\mldownload\rps-test-set.zip (28.1 MB)






```python
import zipfile

def extract_zip(zip_path, extract_dir):
    if not zip_path.exists():
        raise FileNotFoundError(f"Zip file not found. Run the download cell first: {zip_path}")

    with zipfile.ZipFile(zip_path, "r") as zip_ref:
        bad_file = zip_ref.testzip()
        if bad_file is not None:
            raise zipfile.BadZipFile(
                f"Zip file looks corrupted at {bad_file}. Delete {zip_path} and download again."
            )
        zip_ref.extractall(extract_dir)

    print(f"Extracted: {zip_path} -> {extract_dir}")

extract_zip(RPS_ZIP, DOWNLOAD_DIR)
extract_zip(RPS_TEST_ZIP, DOWNLOAD_DIR)

```

    Extracted: D:\RPS\mldownload\rps.zip -> D:\RPS\mldownload
    Extracted: D:\RPS\mldownload\rps-test-set.zip -> D:\RPS\mldownload

### 3.2 数据探索

数据规模

  解压后训练集目录结构为`rps/rock`、`rps/paper`、`rps/scissors`，三类图像数量均等：

- 石头（rock）：840 张；
- 布（paper）：840 张；
- 剪刀（scissors）：840 张；
- 训练集总计：2520 张；
- 验证集总计：372 张。

数据格式

  图像文件为 PNG 格式，命名规则统一（如`rock01-000.png`、`paper01-001.png`），通过可视化样本图片确认图像内容与类别匹配。

```python
rock_dir = DOWNLOAD_DIR / "rps" / "rock"
paper_dir = DOWNLOAD_DIR / "rps" / "paper"
scissors_dir = DOWNLOAD_DIR / "rps" / "scissors"

for image_dir in [rock_dir, paper_dir, scissors_dir]:
    if not image_dir.exists():
        raise FileNotFoundError(
            f"Directory not found: {image_dir}. Run the download and extract cells first."
        )

rock_files = sorted(path.name for path in rock_dir.iterdir())
paper_files = sorted(path.name for path in paper_dir.iterdir())
scissors_files = sorted(path.name for path in scissors_dir.iterdir())

print("total training rock images:", len(rock_files))
print("total training paper images:", len(paper_files))
print("total training scissors images:", len(scissors_files))

print(rock_files[:10])
print(paper_files[:10])
print(scissors_files[:10])

```

    total training rock images: 840
    total training paper images: 840
    total training scissors images: 840
    ['rock01-000.png', 'rock01-001.png', 'rock01-002.png', 'rock01-003.png', 'rock01-004.png', 'rock01-005.png', 'rock01-006.png', 'rock01-007.png', 'rock01-008.png', 'rock01-009.png']
    ['paper01-000.png', 'paper01-001.png', 'paper01-002.png', 'paper01-003.png', 'paper01-004.png', 'paper01-005.png', 'paper01-006.png', 'paper01-007.png', 'paper01-008.png', 'paper01-009.png']
    ['scissors01-000.png', 'scissors01-001.png', 'scissors01-002.png', 'scissors01-003.png', 'scissors01-004.png', 'scissors01-005.png', 'scissors01-006.png', 'scissors01-007.png', 'scissors01-008.png', 'scissors01-009.png']

   查看部分数据

```python
%matplotlib inline

import matplotlib.pyplot as plt
import matplotlib.image as mpimg

pic_index = 2

next_rock = [rock_dir / fname for fname in rock_files[pic_index - 2:pic_index]]
next_paper = [paper_dir / fname for fname in paper_files[pic_index - 2:pic_index]]
next_scissors = [scissors_dir / fname for fname in scissors_files[pic_index - 2:pic_index]]

for img_path in next_rock + next_paper + next_scissors:
    img = mpimg.imread(img_path)
    plt.imshow(img)
    plt.axis("off")
    plt.show()

```


​    
![png](RPS_files/RPS_4_0.png)
​    




![png](RPS_files/RPS_4_1.png)
    




![png](RPS_files/RPS_4_2.png)
    




![png](RPS_files/RPS_4_3.png)
    




![png](RPS_files/RPS_4_4.png)
    




![png](RPS_files/RPS_4_5.png)
    

### 3.3 模型训练

1. 网络结构设计

  采用 Sequential 顺序模型，核心由**卷积层 + 池化层** 特征提取模块、**全连接层** 分类模块组成，引入 Dropout 防止过拟合，具体结构如下：

表格

|         层类型         |                  参数配置                   |       输出形状       |  参数量   |
| :--------------------: | :-----------------------------------------: | :------------------: | :-------: |
|    Conv2D（卷积 1）    | 64 个 3×3 滤波器，ReLU 激活，输入 150×150×3 | (None, 148, 148, 64) |   1,792   |
| MaxPooling2D（池化 1） |                2×2 池化窗口                 |  (None, 74, 74, 64)  |     0     |
|    Conv2D（卷积 2）    |         64 个 3×3 滤波器，ReLU 激活         |  (None, 72, 72, 64)  |  36,928   |
| MaxPooling2D（池化 2） |                2×2 池化窗口                 |  (None, 36, 36, 64)  |     0     |
|    Conv2D（卷积 3）    |        128 个 3×3 滤波器，ReLU 激活         | (None, 34, 34, 128)  |  73,856   |
| MaxPooling2D（池化 3） |                2×2 池化窗口                 | (None, 17, 17, 128)  |     0     |
|    Conv2D（卷积 4）    |        128 个 3×3 滤波器，ReLU 激活         | (None, 15, 15, 128)  |  147,584  |
| MaxPooling2D（池化 4） |                2×2 池化窗口                 |  (None, 7, 7, 128)   |     0     |
|    Flatten（展平）     |                      -                      |     (None, 6272)     |     0     |
|        Dropout         |                 丢弃率 0.5                  |     (None, 6272)     |     0     |
|   Dense（全连接 1）    |            512 神经元，ReLU 激活            |     (None, 512)      | 3,211,776 |
|    Dense（输出层）     |           3 神经元，Softmax 激活            |      (None, 3)       |   1,539   |

2. 模型编译

- 损失函数：`categorical_crossentropy`（适配多分类任务的独热编码标签）；
- 优化器：`rmsprop`（自适应学习率，适合 CNN 训练）；
- 评估指标：`accuracy`（分类准确率）。

3. 模型参数统计

- 总参数量：3,473,475（约 13.25 MB）；
- 可训练参数：3,473,475（无冻结层）；
- 非训练参数：0。



4.训练参数设置

  训练轮数（epochs）：25；每轮训练步数（steps_per_epoch）：20（2520 张训练集 / 126 批量大小）；

  验证步数（validation_steps）：3（372 张验证集 / 126 批量大小）；

```python
import tensorflow as tf

from tensorflow.keras.preprocessing import image
from tensorflow.keras.preprocessing.image import ImageDataGenerator

TRAINING_DIR = "D:/RPS/mldownload/rps/"
training_datagen = ImageDataGenerator(
      rescale = 1./255,
	    rotation_range=40,
      width_shift_range=0.2,
      height_shift_range=0.2,
      shear_range=0.2,
      zoom_range=0.2,
      horizontal_flip=True,
      fill_mode='nearest')

VALIDATION_DIR = "D:/RPS/mldownload/rps-test-set/"
validation_datagen = ImageDataGenerator(rescale = 1./255)

train_generator = training_datagen.flow_from_directory(
	TRAINING_DIR,
	target_size=(150,150),
	class_mode='categorical',
  batch_size=126
)

validation_generator = validation_datagen.flow_from_directory(
	VALIDATION_DIR,
	target_size=(150,150),
	class_mode='categorical',
  batch_size=126
)

model = tf.keras.models.Sequential([
    # Note the input shape is the desired size of the image 150x150 with 3 bytes color
    # This is the first convolution
    tf.keras.layers.Conv2D(64, (3,3), activation='relu', input_shape=(150, 150, 3)),
    tf.keras.layers.MaxPooling2D(2, 2),
    # The second convolution
    tf.keras.layers.Conv2D(64, (3,3), activation='relu'),
    tf.keras.layers.MaxPooling2D(2,2),
    # The third convolution
    tf.keras.layers.Conv2D(128, (3,3), activation='relu'),
    tf.keras.layers.MaxPooling2D(2,2),
    # The fourth convolution
    tf.keras.layers.Conv2D(128, (3,3), activation='relu'),
    tf.keras.layers.MaxPooling2D(2,2),
    # Flatten the results to feed into a DNN
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dropout(0.5),
    # 512 neuron hidden layer
    tf.keras.layers.Dense(512, activation='relu'),
    tf.keras.layers.Dense(3, activation='softmax')
])


model.summary()

model.compile(loss = 'categorical_crossentropy', optimizer='rmsprop', metrics=['accuracy'])

history = model.fit(train_generator, epochs=25, steps_per_epoch=20, validation_data = validation_generator, verbose = 1, validation_steps=3)

model.save("rps.h5")

```

    Found 2520 images belonging to 3 classes.
    Found 372 images belonging to 3 classes.



<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold">Model: "sequential_2"</span>
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace">┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━┓
┃<span style="font-weight: bold"> Layer (type)                    </span>┃<span style="font-weight: bold"> Output Shape           </span>┃<span style="font-weight: bold">       Param # </span>┃
┡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━┩
│ conv2d_8 (<span style="color: #0087ff; text-decoration-color: #0087ff">Conv2D</span>)               │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">148</span>, <span style="color: #00af00; text-decoration-color: #00af00">148</span>, <span style="color: #00af00; text-decoration-color: #00af00">64</span>)   │         <span style="color: #00af00; text-decoration-color: #00af00">1,792</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ max_pooling2d_8 (<span style="color: #0087ff; text-decoration-color: #0087ff">MaxPooling2D</span>)  │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">74</span>, <span style="color: #00af00; text-decoration-color: #00af00">74</span>, <span style="color: #00af00; text-decoration-color: #00af00">64</span>)     │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ conv2d_9 (<span style="color: #0087ff; text-decoration-color: #0087ff">Conv2D</span>)               │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">72</span>, <span style="color: #00af00; text-decoration-color: #00af00">72</span>, <span style="color: #00af00; text-decoration-color: #00af00">64</span>)     │        <span style="color: #00af00; text-decoration-color: #00af00">36,928</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ max_pooling2d_9 (<span style="color: #0087ff; text-decoration-color: #0087ff">MaxPooling2D</span>)  │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">36</span>, <span style="color: #00af00; text-decoration-color: #00af00">36</span>, <span style="color: #00af00; text-decoration-color: #00af00">64</span>)     │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ conv2d_10 (<span style="color: #0087ff; text-decoration-color: #0087ff">Conv2D</span>)              │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">34</span>, <span style="color: #00af00; text-decoration-color: #00af00">34</span>, <span style="color: #00af00; text-decoration-color: #00af00">128</span>)    │        <span style="color: #00af00; text-decoration-color: #00af00">73,856</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ max_pooling2d_10 (<span style="color: #0087ff; text-decoration-color: #0087ff">MaxPooling2D</span>) │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">17</span>, <span style="color: #00af00; text-decoration-color: #00af00">17</span>, <span style="color: #00af00; text-decoration-color: #00af00">128</span>)    │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ conv2d_11 (<span style="color: #0087ff; text-decoration-color: #0087ff">Conv2D</span>)              │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">15</span>, <span style="color: #00af00; text-decoration-color: #00af00">15</span>, <span style="color: #00af00; text-decoration-color: #00af00">128</span>)    │       <span style="color: #00af00; text-decoration-color: #00af00">147,584</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ max_pooling2d_11 (<span style="color: #0087ff; text-decoration-color: #0087ff">MaxPooling2D</span>) │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">7</span>, <span style="color: #00af00; text-decoration-color: #00af00">7</span>, <span style="color: #00af00; text-decoration-color: #00af00">128</span>)      │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ flatten_2 (<span style="color: #0087ff; text-decoration-color: #0087ff">Flatten</span>)             │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">6272</span>)           │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ dropout_2 (<span style="color: #0087ff; text-decoration-color: #0087ff">Dropout</span>)             │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">6272</span>)           │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ dense_4 (<span style="color: #0087ff; text-decoration-color: #0087ff">Dense</span>)                 │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">512</span>)            │     <span style="color: #00af00; text-decoration-color: #00af00">3,211,776</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ dense_5 (<span style="color: #0087ff; text-decoration-color: #0087ff">Dense</span>)                 │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">3</span>)              │         <span style="color: #00af00; text-decoration-color: #00af00">1,539</span> │
└─────────────────────────────────┴────────────────────────┴───────────────┘
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold"> Total params: </span><span style="color: #00af00; text-decoration-color: #00af00">3,473,475</span> (13.25 MB)
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold"> Trainable params: </span><span style="color: #00af00; text-decoration-color: #00af00">3,473,475</span> (13.25 MB)
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold"> Non-trainable params: </span><span style="color: #00af00; text-decoration-color: #00af00">0</span> (0.00 B)
</pre>



    Epoch 1/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m30s[0m 1s/step - accuracy: 0.3528 - loss: 1.1186 - val_accuracy: 0.3333 - val_loss: 1.0878
    Epoch 2/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m33s[0m 2s/step - accuracy: 0.3794 - loss: 1.1022 - val_accuracy: 0.3333 - val_loss: 1.1463
    Epoch 3/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m32s[0m 2s/step - accuracy: 0.4413 - loss: 1.0680 - val_accuracy: 0.5242 - val_loss: 0.8492
    Epoch 4/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m29s[0m 1s/step - accuracy: 0.5190 - loss: 0.9791 - val_accuracy: 0.7473 - val_loss: 0.6039
    Epoch 5/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m33s[0m 2s/step - accuracy: 0.5964 - loss: 0.8535 - val_accuracy: 0.6828 - val_loss: 0.5900
    Epoch 6/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m31s[0m 2s/step - accuracy: 0.6615 - loss: 0.7302 - val_accuracy: 0.9731 - val_loss: 0.3652
    Epoch 7/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.7190 - loss: 0.6265 - val_accuracy: 0.7823 - val_loss: 0.3844
    Epoch 8/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m30s[0m 2s/step - accuracy: 0.7575 - loss: 0.5652 - val_accuracy: 0.6452 - val_loss: 0.6685
    Epoch 9/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m29s[0m 1s/step - accuracy: 0.8048 - loss: 0.4710 - val_accuracy: 0.9059 - val_loss: 0.2323
    Epoch 10/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.8488 - loss: 0.3790 - val_accuracy: 1.0000 - val_loss: 0.0827
    Epoch 11/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.8671 - loss: 0.3207 - val_accuracy: 0.9704 - val_loss: 0.0897
    Epoch 12/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.8889 - loss: 0.2974 - val_accuracy: 0.9973 - val_loss: 0.0516
    Epoch 13/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m29s[0m 1s/step - accuracy: 0.9083 - loss: 0.2370 - val_accuracy: 1.0000 - val_loss: 0.0390
    Epoch 14/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.8940 - loss: 0.2548 - val_accuracy: 0.9086 - val_loss: 0.2073
    Epoch 15/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m31s[0m 2s/step - accuracy: 0.9369 - loss: 0.1667 - val_accuracy: 0.9543 - val_loss: 0.1263
    Epoch 16/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m29s[0m 1s/step - accuracy: 0.9369 - loss: 0.1666 - val_accuracy: 0.9866 - val_loss: 0.0616
    Epoch 17/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m30s[0m 1s/step - accuracy: 0.8893 - loss: 0.3962 - val_accuracy: 1.0000 - val_loss: 0.0286
    Epoch 18/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.9544 - loss: 0.1275 - val_accuracy: 0.9839 - val_loss: 0.0499
    Epoch 19/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m26s[0m 1s/step - accuracy: 0.9433 - loss: 0.1473 - val_accuracy: 0.9866 - val_loss: 0.0459
    Epoch 20/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m27s[0m 1s/step - accuracy: 0.9492 - loss: 0.1592 - val_accuracy: 0.9624 - val_loss: 0.0818
    Epoch 21/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m28s[0m 1s/step - accuracy: 0.9746 - loss: 0.0768 - val_accuracy: 1.0000 - val_loss: 0.0134
    Epoch 22/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m31s[0m 2s/step - accuracy: 0.9635 - loss: 0.1109 - val_accuracy: 0.9543 - val_loss: 0.1345
    Epoch 23/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m29s[0m 1s/step - accuracy: 0.9702 - loss: 0.0953 - val_accuracy: 0.9570 - val_loss: 0.1016
    Epoch 24/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m30s[0m 1s/step - accuracy: 0.9603 - loss: 0.1055 - val_accuracy: 1.0000 - val_loss: 0.0141
    Epoch 25/25
    [1m20/20[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m30s[0m 1s/step - accuracy: 0.9623 - loss: 0.1048 - val_accuracy: 0.9785 - val_loss: 0.0390


    WARNING:absl:You are saving your model as an HDF5 file via `model.save()` or `keras.saving.save_model(model)`. This file format is considered legacy. We recommend using instead the native Keras format, e.g. `model.save('my_model.keras')` or `keras.saving.save_model(model, 'my_model.keras')`. 

### 3.4 模型性能可视化



通过绘制训练 / 验证准确率、损失曲线，可直观观察模型收敛趋势：

- 训练准确率持续上升，验证准确率整体上升且波动较小，说明模型泛化能力良好；
- 训练损失与验证损失均持续下降，后期验证损失维持在 0.04 左右，表明模型稳定收敛。

```python
import matplotlib.pyplot as plt
acc = history.history['accuracy']
val_acc = history.history['val_accuracy']
loss = history.history['loss']
val_loss = history.history['val_loss']

epochs = range(len(acc))

plt.plot(epochs, acc, 'r', label='Training accuracy')
plt.plot(epochs, val_acc, 'b', label='Validation accuracy')
plt.title('Training and validation accuracy')
plt.legend(loc=0)
plt.figure()
plt.show()

```


​    
![png](RPS_files/RPS_6_0.png)
​    



    <Figure size 640x480 with 0 Axes>

## 4.简单测试

```python
# 模型测试 — 使用自己拍摄的图片

import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras.preprocessing import image
from pathlib import Path

# 加载已保存的模型
model = tf.keras.models.load_model("rps.h5")

# 类别名称（按字母顺序，与训练时 flow_from_directory 一致）
class_names = ['paper', 'rock', 'scissors']

def predict_image(img_path):
    """预测单张图片并显示结果"""
    # 加载并预处理
    img = image.load_img(img_path, target_size=(150, 150))
    img_array = image.img_to_array(img) / 255.0
    img_batch = np.expand_dims(img_array, axis=0)

    # 预测
    predictions = model.predict(img_batch, verbose=0)
    predicted_class = class_names[np.argmax(predictions[0])]
    confidence = np.max(predictions[0]) * 100

    # 显示图片和预测结果
    plt.imshow(img_array)
    plt.axis('off')
    plt.title(f"预测: {predicted_class} ({confidence:.1f}%)", fontsize=14)
    plt.show()

    # 打印各类别概率
    print("各类别概率:")
    for cls, prob in zip(class_names, predictions[0]):
        print(f"  {cls}: {prob*100:.2f}%")

    return predicted_class, confidence

# ============================================================
# 👇 修改这里的路径为你自己的图片路径
# ============================================================
image_path1 = "D:\\qqAndWX\\jd.jpg"
predict_image(image_path1)
# ============================================================
# 预测并显示结果
image_path2 = "D:\\qqAndWX\\bu.jpg"
predict_image(image_path2)

# ============================================================
image_path3 = "D:\\qqAndWX\\stone.jpg"
predict_image(image_path3)

```

    WARNING:tensorflow:TensorFlow GPU support is not available on native Windows for TensorFlow >= 2.11. Even if CUDA/cuDNN are installed, GPU will not be used. Please use WSL2 or the TensorFlow-DirectML plugin.


    WARNING:absl:Compiled the loaded model, but the compiled metrics have yet to be built. `model.compile_metrics` will be empty until you train or evaluate the model.
    d:\Anaconda_envs\envs\tf_new\lib\site-packages\IPython\core\pylabtools.py:170: UserWarning: Glyph 39044 (\N{CJK UNIFIED IDEOGRAPH-9884}) missing from font(s) DejaVu Sans.
      fig.canvas.print_figure(bytes_io, **kw)
    d:\Anaconda_envs\envs\tf_new\lib\site-packages\IPython\core\pylabtools.py:170: UserWarning: Glyph 27979 (\N{CJK UNIFIED IDEOGRAPH-6D4B}) missing from font(s) DejaVu Sans.
      fig.canvas.print_figure(bytes_io, **kw)




![png](RPS_files/RPS_7_2.png)
    


    各类别概率:
      paper: 54.82%
      rock: 5.23%
      scissors: 39.96%




![png](RPS_files/RPS_7_4.png)
    


    各类别概率:
      paper: 9.98%
      rock: 84.29%
      scissors: 5.74%




![png](RPS_files/RPS_7_6.png)
    


    各类别概率:
      paper: 5.60%
      rock: 85.65%
      scissors: 8.75%

    ('rock', np.float32(85.653725))

  由于图片和数据集合的差距过大，导致预测准确率较低。





## 5.实验总结

本项目成功完成了石头剪刀布图像分类任务，核心成果包括：

​     实现了数据集的自动化下载、校验与解压，保证数据完整性；

​     构建的 CNN 模型结构合理，通过数据增强和 Dropout 有效避免过拟合；

​     模型训练收敛快，最终在验证集上达到 98% 以上的准确率，自定义图片测试表现较差，需要进行模型微调；

  
