# compose入门

1. @Composable 声明compose函数，用来定义UI,如果需要预览，需要@Preview注解



认识容器和组件

组件：Button

|   组件名    |   作用   |                  例子                   |
| :---------: | :------: | :-------------------------------------: |
|   `Text`    | 显示文字 |         `Text("Hello Android")`         |
|  `Button`   |   按钮   | `Button(onClick = {}) { Text("点击") }` |
|   `Image`   | 显示图片 |             展示图标 / 照片             |
| `TextField` |  输入框  |            用户名、密码输入             |
| `Checkbox`  |  选择框  |              同意协议勾选               |

 2. 容器

Box容器 层叠容器

​     核心属性

- `contentAlignment = Alignment.Center` → 内部内容**居中**
- `Modifier.fillMaxSize()` → 占满整个屏幕





 Column容器 垂直容器

​      `horizontalAlignment = Alignment.CenterHorizontally` → 水平居中

`   verticalArrangement = Arrangement.Center` → 垂直居中





Row容器

 `verticalAlignment = Alignment.CenterVertically` → 垂直居中

`horizontalArrangement = Arrangement.Center` → 水平居中





Modifier修饰符

用来设置：大小、边距、背景、位置、点击事件

例如

```
modifier = Modifier.padding(innerPadding)
传入内边距的修饰情况
```



3.实现

@Composable 

​    是Compose的函数标记，可以在 Compose 的 UI 树中被调用来生成界面元素

被该注解标记的函数只能在在 Compose 上下文，比如 `setContent { ... }` 或其他 `@Composable` 函数里被调用

<img width="1138" height="516" alt="image" src="https://github.com/user-attachments/assets/e7b6e591-fe53-489f-8997-4b454daba99d" />



​     可组合函数像其他函数一致，例如下列通过循环显示多个文本内容

<img width="1134" height="313" alt="image" src="https://github.com/user-attachments/assets/1b7a11cf-d640-4bbb-bd04-58ab04e0acc3" />



@preview 

 这个注解只能标注在 `@Composable` 函数上，@Composable 函数可以在预览窗口里显示 UI，而不需要运行到设备或模拟器。

```
showBackground 默认false  是否显示背景
widthDp/heightDp   预览的尺寸
showSystemUi 默认false  显示状态栏/导航栏
```



​      修改modifier的padding内边距的水平和垂直情况

<img width="1176" height="693" alt="image" src="https://github.com/user-attachments/assets/4b52a440-7dfd-4ebe-925c-f65049122814" />



添加button


​    普通的变量改变不会被Compose捕捉并更新状态。这样点击文件就会发生改变

​      remember { mutableStateOf("Click me") } 使用remember在Composable 的生命周期内 记住值，防止重组（composable函数可能执行多次）时被重置；

​      返回一个 **可观察的状态对象**：，，这样当 value 改变时，Compose 会自动重新渲染使用它的 UI。

   如果我们将变量修改为布尔值，则可以实现点击的动态变化。初始信息为“change to one”,点击后变成“change to two”,再次点击依次往复

<img width="1236" height="520" alt="image" src="https://github.com/user-attachments/assets/5451dcd9-c9dd-4c58-8225-a68d181cf712" />
