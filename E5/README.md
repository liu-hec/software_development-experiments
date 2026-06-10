```python
import tarfile
from pathlib import Path

import numpy as np
import tensorflow as tf

# TensorFlow 官方花卉数据集。第一次运行时会自动下载，之后会复用本地缓存。
FLOWER_URL = "https://storage.googleapis.com/download.tensorflow.org/example_images/flower_photos.tgz"

print("TensorFlow 版本:", tf.__version__)



# 数据目录配置：
# - DATA_DIR = None：自动下载并使用 TensorFlow 官方 flowers 数据集。
# - DATA_DIR = r"D:\path\to\my_images"：使用你自己的图片分类目录。
#
# 自定义图片目录需要按类别分文件夹，例如：
# my_images/
#   daisy/
#     1.jpg
#   roses/
#     2.jpg
DATA_DIR = None

# 导出目录。训练完成后会在这里生成 model.tflite、labels.txt 和 flower_classifier.keras。
EXPORT_DIR = "exported_flower_model"

# 训练参数。教程演示可以先用 3 到 5 个 epoch；如果使用自己的数据，可以适当增加。
EPOCHS = 5
BATCH_SIZE = 32
IMAGE_SIZE = 224
LEARNING_RATE = 1e-3

# TFLite 量化方式：
# - "dynamic"：默认推荐，模型更小，通常最容易成功。
# - "float16"：适合部分支持 float16 的设备。
# - "int8"：体积更小，但需要代表性数据集，转换要求更严格。
# - "none"：不量化，保留浮点模型。
QUANTIZATION = "dynamic"

# 固定随机种子，方便训练/验证划分尽量可复现。
SEED = 123




def load_flower_datasets(data_dir, image_size, batch_size, seed):
    # 如果没有传入自定义数据目录，就下载 TensorFlow 官方 flower_photos 数据集。
    if data_dir is None:
        archive_path = tf.keras.utils.get_file(
            "flower_photos.tgz",
            FLOWER_URL,
            extract=False,
        )
        archive_path = Path(archive_path)

        # Keras 可能已经缓存了解压后的目录；先检查常见位置，避免重复解压。
        candidates = [
            archive_path.parent / "flower_photos",
            archive_path.parent / "flower_photos_extracted" / "flower_photos",
        ]
        data_dir = next((path for path in candidates if path.exists()), None)
        if data_dir is None:
            with tarfile.open(archive_path, "r:gz") as tar:
                tar.extractall(archive_path.parent / "flower_photos_extracted")
            data_dir = archive_path.parent / "flower_photos_extracted" / "flower_photos"
    else:
        data_dir = Path(data_dir)

    # 从目录读取图片。目录下的每个子文件夹会被当作一个类别。
    train_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.2,
        subset="training",
        seed=seed,
        image_size=(image_size, image_size),
        batch_size=batch_size,
    )
    val_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.2,
        subset="validation",
        seed=seed,
        image_size=(image_size, image_size),
        batch_size=batch_size,
    )
    class_names = train_ds.class_names

    # 原始 validation 部分再拆成验证集和测试集：验证集用于训练过程中观察效果，测试集用于最后评估。
    val_batches = int(tf.data.experimental.cardinality(val_ds).numpy())
    test_ds = val_ds.take(val_batches // 2)
    val_ds = val_ds.skip(val_batches // 2)

    # cache/prefetch 可以减少数据读取等待；shuffle 只用于训练集。
    autotune = tf.data.AUTOTUNE
    train_ds = train_ds.cache().shuffle(1000, seed=seed).prefetch(autotune)
    val_ds = val_ds.cache().prefetch(autotune)
    test_ds = test_ds.cache().prefetch(autotune)
    return train_ds, val_ds, test_ds, class_names


# 加载数据集并查看类别名称。
train_ds, val_ds, test_ds, class_names = load_flower_datasets(
    DATA_DIR,
    IMAGE_SIZE,
    BATCH_SIZE,
    SEED,
)

print("类别数量:", len(class_names))
print("类别名称:", class_names)



def build_model(num_classes, image_size, learning_rate):
    # 输入图片尺寸固定为 IMAGE_SIZE x IMAGE_SIZE x 3。
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")

    # MobileNetV2 有自己的预处理方式，这里把像素值转换到模型期望的范围。
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)

    # include_top=False 表示不要 ImageNet 原始的 1000 类分类头，只保留特征提取部分。
    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(image_size, image_size, 3),
        include_top=False,
        weights="imagenet",
        pooling="avg",
    )

    # 冻结预训练模型参数，只训练后面的 Dense 分类层。
    base_model.trainable = False
    x = base_model(x, training=False)
    x = tf.keras.layers.Dropout(0.2)(x)

    # 输出维度等于类别数量，softmax 输出每个类别的概率。
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax", name="predictions")(x)
    model = tf.keras.Model(inputs, outputs)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=["accuracy"],
    )
    return model


# 创建模型并打印结构。第一次运行会下载 MobileNetV2 的 ImageNet 预训练权重。
model = build_model(len(class_names), IMAGE_SIZE, LEARNING_RATE)
model.summary()


# 开始训练。history 中会保存每个 epoch 的 loss、accuracy、val_loss、val_accuracy。
history = model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS)


# 使用测试集评估模型。测试集没有参与训练，用于更客观地观察最终效果。
loss, accuracy = model.evaluate(test_ds)
print(f"test_loss={loss:.4f}, test_accuracy={accuracy:.4f}")


def convert_to_tflite(model, quantization, representative_ds):
    # 从 Keras 模型创建 TFLite 转换器。
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    if quantization == "dynamic":
        # 动态范围量化：最常用、最容易成功的压缩方式。
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    elif quantization == "float16":
        # float16 量化：权重使用半精度浮点数，适合部分移动端/GPU 场景。
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
    elif quantization == "int8":
        # int8 全整数量化：体积更小，但需要代表性数据集校准输入分布。
        converter.optimizations = [tf.lite.Optimize.DEFAULT]

        def representative_data_gen():
            for images, _ in representative_ds.take(100):
                for image in images:
                    yield [tf.expand_dims(tf.cast(image, tf.float32), 0)]

        converter.representative_dataset = representative_data_gen
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.uint8
        converter.inference_output_type = tf.uint8
    elif quantization != "none":
        raise ValueError(f"Unsupported quantization mode: {quantization}")

    return converter.convert()


# 创建导出目录。
export_dir = Path(EXPORT_DIR)
export_dir.mkdir(parents=True, exist_ok=True)

# 保存标签文件。部署时需要 labels.txt 把模型输出编号映射回类别名称。
labels_path = export_dir / "labels.txt"
labels_path.write_text("\n".join(class_names) + "\n", encoding="utf-8")

# 保存 Keras 原始模型，便于以后继续训练或重新转换。
keras_path = export_dir / "flower_classifier.keras"
model.save(keras_path)

# 转换并保存 TFLite 模型。
tflite_model = convert_to_tflite(model, QUANTIZATION, train_ds)
tflite_path = export_dir / "model.tflite"
tflite_path.write_bytes(tflite_model)

print(f"已保存 Keras 模型: {keras_path}")
print(f"已保存 TFLite 模型: {tflite_path}")
print(f"已保存标签文件: {labels_path}")


def smoke_test_tflite(tflite_path, test_ds, class_names):
    # 加载 TFLite 模型并分配张量内存。
    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    # 从测试集中取 8 张图片做快速推理。
    images, labels = next(iter(test_ds.unbatch().batch(8)))
    input_data = tf.cast(images, input_details["dtype"]).numpy()

    # 如果模型是 uint8 输入，需要按照量化参数把图片转换到对应范围。
    if input_details["dtype"] == np.uint8:
        scale, zero_point = input_details["quantization"]
        if scale:
            input_data = images.numpy() / scale + zero_point
            input_data = np.clip(input_data, 0, 255).astype(np.uint8)

    predictions = []
    for image in input_data:
        interpreter.set_tensor(input_details["index"], np.expand_dims(image, 0))
        interpreter.invoke()
        predictions.append(interpreter.get_tensor(output_details["index"])[0])

    predicted_ids = np.argmax(np.asarray(predictions), axis=1)
    for expected, predicted in zip(labels.numpy()[:5], predicted_ids[:5]):
        print(f"真实类别={class_names[expected]}, 预测类别={class_names[predicted]}")


# 运行 TFLite 快速测试。
smoke_test_tflite(tflite_path, test_ds, class_names)

```


    ---------------------------------------------------------------------------
    
    ModuleNotFoundError                       Traceback (most recent call last)
    
    Cell In[1], line 4
          1 import tarfile
          2 from pathlib import Path
    ----> 4 import numpy as np
          5 import tensorflow as tf
          7 # TensorFlow 官方花卉数据集。第一次运行时会自动下载，之后会复用本地缓存。


    ModuleNotFoundError: No module named 'numpy'



```python
import tarfile
from pathlib import Path

import numpy as np
import tensorflow as tf

# TensorFlow 官方花卉数据集。第一次运行时会自动下载，之后会复用本地缓存。
FLOWER_URL = "https://storage.googleapis.com/download.tensorflow.org/example_images/flower_photos.tgz"

print("TensorFlow 版本:", tf.__version__)



# 数据目录配置：
# - DATA_DIR = None：自动下载并使用 TensorFlow 官方 flowers 数据集。
# - DATA_DIR = r"D:\path\to\my_images"：使用你自己的图片分类目录。
#
# 自定义图片目录需要按类别分文件夹，例如：
# my_images/
#   daisy/
#     1.jpg
#   roses/
#     2.jpg
DATA_DIR = None

# 导出目录。训练完成后会在这里生成 model.tflite、labels.txt 和 flower_classifier.keras。
EXPORT_DIR = "exported_flower_model"

# 训练参数。教程演示可以先用 3 到 5 个 epoch；如果使用自己的数据，可以适当增加。
EPOCHS = 5
BATCH_SIZE = 32
IMAGE_SIZE = 224
LEARNING_RATE = 1e-3

# TFLite 量化方式：
# - "dynamic"：默认推荐，模型更小，通常最容易成功。
# - "float16"：适合部分支持 float16 的设备。
# - "int8"：体积更小，但需要代表性数据集，转换要求更严格。
# - "none"：不量化，保留浮点模型。
QUANTIZATION = "dynamic"

# 固定随机种子，方便训练/验证划分尽量可复现。
SEED = 123




def load_flower_datasets(data_dir, image_size, batch_size, seed):
    # 如果没有传入自定义数据目录，就下载 TensorFlow 官方 flower_photos 数据集。
    if data_dir is None:
        archive_path = tf.keras.utils.get_file(
            "flower_photos.tgz",
            FLOWER_URL,
            extract=False,
        )
        archive_path = Path(archive_path)

        # Keras 可能已经缓存了解压后的目录；先检查常见位置，避免重复解压。
        candidates = [
            archive_path.parent / "flower_photos",
            archive_path.parent / "flower_photos_extracted" / "flower_photos",
        ]
        data_dir = next((path for path in candidates if path.exists()), None)
        if data_dir is None:
            with tarfile.open(archive_path, "r:gz") as tar:
                tar.extractall(archive_path.parent / "flower_photos_extracted")
            data_dir = archive_path.parent / "flower_photos_extracted" / "flower_photos"
    else:
        data_dir = Path(data_dir)

    # 从目录读取图片。目录下的每个子文件夹会被当作一个类别。
    train_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.2,
        subset="training",
        seed=seed,
        image_size=(image_size, image_size),
        batch_size=batch_size,
    )
    val_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.2,
        subset="validation",
        seed=seed,
        image_size=(image_size, image_size),
        batch_size=batch_size,
    )
    class_names = train_ds.class_names

    # 原始 validation 部分再拆成验证集和测试集：验证集用于训练过程中观察效果，测试集用于最后评估。
    val_batches = int(tf.data.experimental.cardinality(val_ds).numpy())
    test_ds = val_ds.take(val_batches // 2)
    val_ds = val_ds.skip(val_batches // 2)

    # cache/prefetch 可以减少数据读取等待；shuffle 只用于训练集。
    autotune = tf.data.AUTOTUNE
    train_ds = train_ds.cache().shuffle(1000, seed=seed).prefetch(autotune)
    val_ds = val_ds.cache().prefetch(autotune)
    test_ds = test_ds.cache().prefetch(autotune)
    return train_ds, val_ds, test_ds, class_names


# 加载数据集并查看类别名称。
train_ds, val_ds, test_ds, class_names = load_flower_datasets(
    DATA_DIR,
    IMAGE_SIZE,
    BATCH_SIZE,
    SEED,
)

print("类别数量:", len(class_names))
print("类别名称:", class_names)



def build_model(num_classes, image_size, learning_rate):
    # 输入图片尺寸固定为 IMAGE_SIZE x IMAGE_SIZE x 3。
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")

    # MobileNetV2 有自己的预处理方式，这里把像素值转换到模型期望的范围。
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)

    # include_top=False 表示不要 ImageNet 原始的 1000 类分类头，只保留特征提取部分。
    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(image_size, image_size, 3),
        include_top=False,
        weights="imagenet",
        pooling="avg",
    )

    # 冻结预训练模型参数，只训练后面的 Dense 分类层。
    base_model.trainable = False
    x = base_model(x, training=False)
    x = tf.keras.layers.Dropout(0.2)(x)

    # 输出维度等于类别数量，softmax 输出每个类别的概率。
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax", name="predictions")(x)
    model = tf.keras.Model(inputs, outputs)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=["accuracy"],
    )
    return model


# 创建模型并打印结构。第一次运行会下载 MobileNetV2 的 ImageNet 预训练权重。
model = build_model(len(class_names), IMAGE_SIZE, LEARNING_RATE)
model.summary()


# 开始训练。history 中会保存每个 epoch 的 loss、accuracy、val_loss、val_accuracy。
history = model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS)


# 使用测试集评估模型。测试集没有参与训练，用于更客观地观察最终效果。
loss, accuracy = model.evaluate(test_ds)
print(f"test_loss={loss:.4f}, test_accuracy={accuracy:.4f}")


def convert_to_tflite(model, quantization, representative_ds):
    # 从 Keras 模型创建 TFLite 转换器。
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    if quantization == "dynamic":
        # 动态范围量化：最常用、最容易成功的压缩方式。
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    elif quantization == "float16":
        # float16 量化：权重使用半精度浮点数，适合部分移动端/GPU 场景。
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
    elif quantization == "int8":
        # int8 全整数量化：体积更小，但需要代表性数据集校准输入分布。
        converter.optimizations = [tf.lite.Optimize.DEFAULT]

        def representative_data_gen():
            for images, _ in representative_ds.take(100):
                for image in images:
                    yield [tf.expand_dims(tf.cast(image, tf.float32), 0)]

        converter.representative_dataset = representative_data_gen
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.uint8
        converter.inference_output_type = tf.uint8
    elif quantization != "none":
        raise ValueError(f"Unsupported quantization mode: {quantization}")

    return converter.convert()


# 创建导出目录。
export_dir = Path(EXPORT_DIR)
export_dir.mkdir(parents=True, exist_ok=True)

# 保存标签文件。部署时需要 labels.txt 把模型输出编号映射回类别名称。
labels_path = export_dir / "labels.txt"
labels_path.write_text("\n".join(class_names) + "\n", encoding="utf-8")

# 保存 Keras 原始模型，便于以后继续训练或重新转换。
keras_path = export_dir / "flower_classifier.keras"
model.save(keras_path)

# 转换并保存 TFLite 模型。
tflite_model = convert_to_tflite(model, QUANTIZATION, train_ds)
tflite_path = export_dir / "model.tflite"
tflite_path.write_bytes(tflite_model)

print(f"已保存 Keras 模型: {keras_path}")
print(f"已保存 TFLite 模型: {tflite_path}")
print(f"已保存标签文件: {labels_path}")


def smoke_test_tflite(tflite_path, test_ds, class_names):
    # 加载 TFLite 模型并分配张量内存。
    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    # 从测试集中取 8 张图片做快速推理。
    images, labels = next(iter(test_ds.unbatch().batch(8)))
    input_data = tf.cast(images, input_details["dtype"]).numpy()

    # 如果模型是 uint8 输入，需要按照量化参数把图片转换到对应范围。
    if input_details["dtype"] == np.uint8:
        scale, zero_point = input_details["quantization"]
        if scale:
            input_data = images.numpy() / scale + zero_point
            input_data = np.clip(input_data, 0, 255).astype(np.uint8)

    predictions = []
    for image in input_data:
        interpreter.set_tensor(input_details["index"], np.expand_dims(image, 0))
        interpreter.invoke()
        predictions.append(interpreter.get_tensor(output_details["index"])[0])

    predicted_ids = np.argmax(np.asarray(predictions), axis=1)
    for expected, predicted in zip(labels.numpy()[:5], predicted_ids[:5]):
        print(f"真实类别={class_names[expected]}, 预测类别={class_names[predicted]}")


# 运行 TFLite 快速测试。
smoke_test_tflite(tflite_path, test_ds, class_names)

```

    TensorFlow 版本: 2.21.0
    Found 3670 files belonging to 5 classes.
    Using 2936 files for training.
    Found 3670 files belonging to 5 classes.
    Using 734 files for validation.
    类别数量: 5
    类别名称: ['daisy', 'dandelion', 'roses', 'sunflowers', 'tulips']
    WARNING:tensorflow:TensorFlow GPU support is not available on native Windows for TensorFlow >= 2.11. Even if CUDA/cuDNN are installed, GPU will not be used. Please use WSL2 or the TensorFlow-DirectML plugin.



<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold">Model: "functional"</span>
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace">┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━┓
┃<span style="font-weight: bold"> Layer (type)                    </span>┃<span style="font-weight: bold"> Output Shape           </span>┃<span style="font-weight: bold">       Param # </span>┃
┡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━┩
│ image (<span style="color: #0087ff; text-decoration-color: #0087ff">InputLayer</span>)              │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">224</span>, <span style="color: #00af00; text-decoration-color: #00af00">224</span>, <span style="color: #00af00; text-decoration-color: #00af00">3</span>)    │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ true_divide (<span style="color: #0087ff; text-decoration-color: #0087ff">TrueDivide</span>)        │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">224</span>, <span style="color: #00af00; text-decoration-color: #00af00">224</span>, <span style="color: #00af00; text-decoration-color: #00af00">3</span>)    │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ subtract (<span style="color: #0087ff; text-decoration-color: #0087ff">Subtract</span>)             │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">224</span>, <span style="color: #00af00; text-decoration-color: #00af00">224</span>, <span style="color: #00af00; text-decoration-color: #00af00">3</span>)    │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ mobilenetv2_1.00_224            │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">1280</span>)           │     <span style="color: #00af00; text-decoration-color: #00af00">2,257,984</span> │
│ (<span style="color: #0087ff; text-decoration-color: #0087ff">Functional</span>)                    │                        │               │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ dropout (<span style="color: #0087ff; text-decoration-color: #0087ff">Dropout</span>)               │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">1280</span>)           │             <span style="color: #00af00; text-decoration-color: #00af00">0</span> │
├─────────────────────────────────┼────────────────────────┼───────────────┤
│ predictions (<span style="color: #0087ff; text-decoration-color: #0087ff">Dense</span>)             │ (<span style="color: #00d7ff; text-decoration-color: #00d7ff">None</span>, <span style="color: #00af00; text-decoration-color: #00af00">5</span>)              │         <span style="color: #00af00; text-decoration-color: #00af00">6,405</span> │
└─────────────────────────────────┴────────────────────────┴───────────────┘
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold"> Total params: </span><span style="color: #00af00; text-decoration-color: #00af00">2,264,389</span> (8.64 MB)
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold"> Trainable params: </span><span style="color: #00af00; text-decoration-color: #00af00">6,405</span> (25.02 KB)
</pre>




<pre style="white-space:pre;overflow-x:auto;line-height:normal;font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><span style="font-weight: bold"> Non-trainable params: </span><span style="color: #00af00; text-decoration-color: #00af00">2,257,984</span> (8.61 MB)
</pre>



    Epoch 1/5
    [1m92/92[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m30s[0m 296ms/step - accuracy: 0.6866 - loss: 0.8346 - val_accuracy: 0.8534 - val_loss: 0.4494
    Epoch 2/5
    [1m92/92[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m27s[0m 297ms/step - accuracy: 0.8518 - loss: 0.4160 - val_accuracy: 0.8770 - val_loss: 0.3693
    Epoch 3/5
    [1m92/92[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m25s[0m 275ms/step - accuracy: 0.8832 - loss: 0.3329 - val_accuracy: 0.8770 - val_loss: 0.3257
    Epoch 4/5
    [1m92/92[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m26s[0m 283ms/step - accuracy: 0.9026 - loss: 0.2896 - val_accuracy: 0.8927 - val_loss: 0.3018
    Epoch 5/5
    [1m92/92[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m27s[0m 294ms/step - accuracy: 0.9220 - loss: 0.2483 - val_accuracy: 0.9110 - val_loss: 0.2787
    [1m11/11[0m [32m━━━━━━━━━━━━━━━━━━━━[0m[37m[0m [1m3s[0m 266ms/step - accuracy: 0.8949 - loss: 0.3250
    test_loss=0.3250, test_accuracy=0.8949
    INFO:tensorflow:Assets written to: C:\Users\86152\AppData\Local\Temp\tmp6o4atz8m\assets


    INFO:tensorflow:Assets written to: C:\Users\86152\AppData\Local\Temp\tmp6o4atz8m\assets


    Saved artifact at 'C:\Users\86152\AppData\Local\Temp\tmp6o4atz8m'. The following endpoints are available:
    
    * Endpoint 'serve'
      args_0 (POSITIONAL_ONLY): TensorSpec(shape=(None, 224, 224, 3), dtype=tf.float32, name='image')
    Output Type:
      TensorSpec(shape=(None, 5), dtype=tf.float32, name=None)
    Captures:
      2374741392400: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741688736: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741691200: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741691024: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741689968: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374720145536: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741695952: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741698416: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741694192: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741697184: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741703696: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741700528: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741702112: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741704224: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741705824: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741707584: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741710400: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741712688: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741708640: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741711456: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741720256: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741716208: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741718144: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741719552: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741720432: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741331456: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741334272: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741336560: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741332512: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741335328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741337792: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741340608: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741342896: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741338848: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741341664: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374741716912: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742200160: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742202800: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742198400: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742200688: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742205440: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742208256: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742210544: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742206496: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742209312: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742210368: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742347088: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742349376: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742345328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742348144: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742197872: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742356064: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742358704: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742354304: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742356592: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742358528: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742429888: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742432176: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742428128: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742430944: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742433408: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742436224: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742438512: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742434464: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742437280: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742432352: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742492784: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742495600: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742491728: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742494544: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742498240: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742501056: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742503344: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742499296: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742502112: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742506688: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742608528: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742606064: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742605888: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742608704: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742441328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742614688: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742617328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742612928: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742615216: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742620672: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742706128: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742704368: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742706480: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742706304: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742617152: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742713696: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742714752: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742711232: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742713168: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742719328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742786992: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742788224: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742789280: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742788752: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742792448: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742795264: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742797552: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742793504: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742796320: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742801424: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742798256: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742799840: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742801952: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742869088: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742719152: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742874720: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742877360: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742872960: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742875248: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742880000: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742879472: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742881760: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742883168: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374742883344: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743002624: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743005440: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743007728: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743003680: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743006496: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743013888: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743012832: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743011600: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743011072: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743013360: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743100224: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743103040: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743105328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743101280: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743104096: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743107968: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743110784: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743113072: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743109024: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743111840: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743010544: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743183904: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743186544: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743182144: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743184432: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743189184: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743192000: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743194288: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743190240: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743193056: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743194112: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743281856: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743284144: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743280096: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743282912: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743181616: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743289248: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743291888: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743287488: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743289776: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743291712: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743395840: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743398128: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743394080: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743396896: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743399360: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743402176: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743404464: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743400416: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743403232: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743398304: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743442352: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743443584: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743444640: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743444112: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743447808: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743450624: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743452912: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743448864: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743451680: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743456256: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743590864: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743589104: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743591216: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743591040: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743407280: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743597024: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743599664: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743595264: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743597552: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743603008: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743672080: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743604064: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743602304: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743670848: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743675776: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743678592: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743680880: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743676832: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743679648: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743673312: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743753120: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743755408: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743755232: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743754880: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743758576: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743761392: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743763680: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743759632: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743762448: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743764912: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743764384: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743766672: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743768080: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743768256: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743685280: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743873616: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743876256: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743871856: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743874144: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743878896: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743881712: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743880480: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743879952: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743882768: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743935984: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743938800: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743941088: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743937040: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743939856: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743947248: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743946192: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743948832: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743944432: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743946720: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743948656: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744052784: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744055072: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744051024: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744053840: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744057712: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744060528: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744062816: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744058768: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374744061584: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374743943904: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750588944: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750591584: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750587184: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750589472: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750594224: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750597040: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750599328: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750595280: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750598096: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750599152: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750670336: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750671392: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750667872: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750670512: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750681952: TensorSpec(shape=(), dtype=tf.resource, name=None)
      2374750680192: TensorSpec(shape=(), dtype=tf.resource, name=None)
    已保存 Keras 模型: exported_flower_model\flower_classifier.keras
    已保存 TFLite 模型: exported_flower_model\model.tflite
    已保存标签文件: exported_flower_model\labels.txt


    d:\Anaconda_envs\envs\tf_new\lib\site-packages\tensorflow\lite\python\interpreter.py:457: UserWarning:     Warning: tf.lite.Interpreter is deprecated and is scheduled for deletion in
        TF 2.20. Please use the LiteRT interpreter from the ai_edge_litert package.
        See the [migration guide](https://ai.google.dev/edge/litert/migration)
        for details.
        
      warnings.warn(_INTERPRETER_DELETION_WARNING)


    真实类别=daisy, 预测类别=daisy
    真实类别=tulips, 预测类别=tulips
    真实类别=sunflowers, 预测类别=sunflowers
    真实类别=daisy, 预测类别=daisy
    真实类别=sunflowers, 预测类别=sunflowers







#   TensorFlow花卉图片分类器模型训练

## 1.实验依赖

  conda创建对应的内核，下载tensorflow 



## 2.实验步骤

###   2.1 数据导入

```
import tarfile
from pathlib import Path

import numpy as np
import tensorflow as tf

# TensorFlow 官方花卉数据集。第一次运行时会自动下载，之后会复用本地缓存。
FLOWER_URL = "https://storage.googleapis.com/download.tensorflow.org/example_images/flower_photos.tgz"

print("TensorFlow 版本:", tf.__version__)

```

   对应的数据存放在C:\Users\你的用户名\.keras\datasets\ 

 ![image-20260603112459411](C:/Users/86152/AppData/Roaming/Typora/typora-user-images/image-20260603112459411.png)





```
DATA_DIR = None # 启用在线自动下载官方花卉数据集 

# 导出目录。训练完成后会在这里生成 model.tflite、labels.txt 和 flower_classifier.keras。
EXPORT_DIR = "exported_flower_model" #自行设置 

# 训练参数。教程演示可以先用 3 到 5 个 epoch；如果使用自己的数据，可以适当增加。
EPOCHS = 5      #训练轮数
BATCH_SIZE = 32   #batch的图片数量 每次训练32张
IMAGE_SIZE = 224  #224 x 224像素 缩放
LEARNING_RATE = 1e-3  #控制模型权重更新的步长 理解为模型每次训练调参的程度或者分量

# TFLite 量化方式：
# - "dynamic"：默认推荐，模型更小，通常最容易成功
QUANTIZATION = "dynamic"

# 固定随机种子，方便训练/验证划分尽量可复现。
SEED = 123


```

```
def load_flower_datasets(data_dir, image_size, batch_size, seed):
    # 如果没有传入自定义数据目录，就下载 TensorFlow 官方 flower_photos 数据集。
    if data_dir is None:
       ......用官方的数据

        # Keras 可能已经缓存了解压后的目录；先检查常见位置，避免重复解压。找不到对应文件解压
        candidates = [
            archive_path.parent / "flower_photos",
            archive_path.parent / "flower_photos_extracted" / "flower_photos",
        ]
        data_dir = next((path for path in candidates if path.exists()), None)
        if data_dir is None:
            with tarfile.open(archive_path, "r:gz") as tar:
                tar.extractall(archive_path.parent / "flower_photos_extracted")
            data_dir = archive_path.parent / "flower_photos_extracted" / "flower_photos"
    else:
        data_dir = Path(data_dir)

    # 从目录读取图片。目录下的每个子文件夹会被当作一个类别。他是每一个文件夹 5个花的分类
    train_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.2,    //20%作为验证集
        subset="training",
        seed=seed,
        image_size=(image_size, image_size),
        batch_size=batch_size,
    )
    val_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.2,
        subset="validation",
        seed=seed,
        image_size=(image_size, image_size),
        batch_size=batch_size,
    )
    class_names = train_ds.class_names

    # 原始 validation 部分再拆成验证集和测试集：验证集用于训练过程中观察效果，测试集用于最后评估（最后训练的模型检验）
    val_batches = int(tf.data.experimental.cardinality(val_ds).numpy()) #返回验证集的总批次batch
    test_ds = val_ds.take(val_batches // 2) 
    val_ds = val_ds.skip(val_batches // 2)

    # cache/prefetch 可以减少数据读取等待；shuffle 只用于训练集 打乱数据。
    autotune = tf.data.AUTOTUNE #AUTOTUNE 让 TensorFlow 在运行时动态调整并行处理参数
    train_ds = train_ds.cache().shuffle(1000, se ed=seed).prefetch(autotune)
    val_ds = val_ds.cache().prefetch(autotune) #prefetch 数据预取与模型训练重叠执行，减少等待时间
    test_ds = test_ds.cache().prefetch(autotune)
    return train_ds, val_ds, test_ds, class_names

```

   下载依赖---->官方数据集（或者本地自己的数据集）------>参数设置（训练轮数，学习权重等）------>载入数据（解压）---->划分测试集/验证集（进一步划分为验证集和测试集）

​    

![image-20260603103817795](C:/Users/86152/AppData/Roaming/Typora/typora-user-images/image-20260603103817795.png)







### 2.2 模型训练

  `MobileNetV2`，训练底座

```
def build_model(num_classes, image_size, learning_rate):

#定义输入层
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")
.......
#输入分类层
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax", name="predictions")(x)
     model = tf.keras.Model(inputs, outputs
     

# 开始训练。history 中会保存每个 epoch 的 loss、accuracy、val_loss、val_accuracy。
history = model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS)

# 使用测试集评估模型。测试集没有参与训练，用于更客观地观察最终效果。
loss, accuracy = model.evaluate(test_ds)
print(f"test_loss={loss:.4f}, test_accuracy={accuracy:.4f}")

     
     
```



   模型切换为TFLite转换器

```
converter = tf.lite.TFLiteConverter.from_keras_model(model)
```

  保存输出

```
tflite_path = export_dir / "model.tflite"
tflite_path.write_bytes(tflite_model)
```





### 2.3 冒烟测试

从测试集获取8张图片，简单的冒烟测试，smoke_test_tflite

```
images, labels = next(iter(test_ds.unbatch().batch(8)))
input_data = tf.cast(images, input_details["dtype"]).numpy() #类型转换
 ....
 for expected, predicted in zip(labels.numpy()[:5], predicted_ids[:5]):
 print(f"真实类别={class_names[expected]}, 预测类别={class_names[predicted]}")

```

生成文件清单

1. `flower_classifier.keras`：原始 Keras 模型文件；
2. `model.tflite`：量化后移动端部署模型；
3. `labels.txt`：分类标签文档。



### 2.4 具体测试

​       将model.tflite 修改为FlowerModel.tflite，替换到E4的start 模块中。 导出apk，下载到手机中进行测试。

​        <img width="410" height="911" alt="image" src="https://github.com/user-attachments/assets/353668bc-25a4-4a2e-af47-d54ebeb1c1b0" />


<img width="410" height="902" alt="image" src="https://github.com/user-attachments/assets/7b1de15a-9177-4e85-86e4-4ab85b9326dc" />



<img width="411" height="903" alt="image" src="https://github.com/user-attachments/assets/0b5d58af-1547-403a-adcb-7ce44f8775a5" />


<img width="417" height="910" alt="image" src="https://github.com/user-attachments/assets/d945f966-e0d8-47ef-9710-ae883eb1c20f" />





## 3.实验总结

​      本次实验成功搭建了一个基于 MobileNetV2 迁移学习的花卉分类器，完成了从数据准备、模型训练、评估到 TFLite 导出的全流程，并进行了有效的冒烟测试。该流程可直接迁移到其他图像分类任务。

​     **环境兼容问题**：tflite-model-maker 依赖老旧 TensorFlow、scann 库，新版 Python 安装报错，实验改用原生 Keras 训练再手动转 TFLite，彻底规避依赖冲突。

​    **冒烟测试的价值**：在正式部署前，用少量样本跑通 TFLite 推理流程，能快速发现模型格式、输入预处理、量化参数等错误，避免严重故障。









