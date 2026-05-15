[1.安装.md](https://github.com/user-attachments/files/27013673/1.md)
1
### 1. Android Studio 安装

1. 下载 Android Studio 4.1 及以上版本（推荐 Panda 3）

    链接：[Download Android Studio & App Tools - Android Developers](https://developer.android.com/studio)  进入下载页面下载即可

    <img width="1050" height="364" alt="image" src="https://github.com/user-attachments/assets/31830154-88ee-4c2c-8cc0-615c5240bbf8" />

<img width="1454" height="647" alt="image" src="https://github.com/user-attachments/assets/8ff5c491-adb5-4504-8004-fe0e810bb70d" />

![Uploading image.png…]()


2. 标准安装，**安装路径禁止包含中文、空格**

<img width="803" height="512" alt="image" src="https://github.com/user-attachments/assets/18de63da-aa8b-43b3-ba07-8b88072889d0" />

      选择D盘 否则C随着开发会越来越大，下载的依赖很多。

3.  Gradle 配置  可以自定义配置或者网络自动下载配置  类比maven

<img width="978" height="723" alt="image" src="https://github.com/user-attachments/assets/06e4c72d-8233-4b82-9815-1f35f32c3451" />

    可以配置自行下载的gradle 同时安装device manager

4. 新建 Empty Views Activity 项目，完成首次编译与运行

<img width="1148" height="425" alt="image" src="https://github.com/user-attachments/assets/67315482-a0f1-468d-9cd3-a89245fb4565" />

 网络问题可以替换distributionUrl,修改为国内镜像源，例如mirrors.cloud.tencent.com/gradle等等

### 2. Anaconda（Jupyter Notebook）安装

1. 下载 Anaconda 安装包

    https://www.anaconda.com/download/  下载 或者使用镜像源下载

2. 安装配置：

    - 路径无中文、无空格
    - 选择 just for me 否则需要管理员权限
    - 使用 Anaconda 作为默认 Python 环境

<img width="643" height="444" alt="image" src="https://github.com/user-attachments/assets/619b67e1-5582-4972-b7ce-83ea0fd12fe6" />



    配置环境变量 

    将安装的地址的加入到系统的环境变量

    ```
    D:\anaconda3
    D:\anaconda3\Scripts
    D:\anaconda3\Library\bin
    D:\anaconda3\Library\mingw-w64\bin
    ```

    

3. 安装验证：

     搜索prompt ,点击anaconda prompt 或者anaconda Powershell Prompt,输入conda  --version

<img width="502" height="210" alt="image" src="https://github.com/user-attachments/assets/84a68ad5-fce6-4a4a-bbd7-7c3614f85059" />

    

    

    

4. 安装和启动 Jupyter Notebook：

    在anaconda 的环境下安装Jupyter Notebook

    4.1 创建虚拟环境 

    ```
    conda create --name jupyter_learn 替换为自己的取名的名字
    conda env list 查看是否创建成功
    ```

<img width="1132" height="415" alt="image" src="https://github.com/user-attachments/assets/4ebafd9d-4216-40e0-94cf-c7c23729beb2" />

    看到my_jupter意味着创建成功

    

    4.2 启动

    ```
    jupyter notebook
    ```



    自动跳转到浏览器，安装成功

    

    默认是c盘目录的内容，如何修改呢？

    ```
    jupyter notebook --generate-config
    ```

    找到对于的配置文件.py  修改ServerApp.notebook_dir

    ```
    c.ServerApp.notebook_dir = 'D:/jupyterProjects'
    ```

<img width="1129" height="421" alt="image" src="https://github.com/user-attachments/assets/f2326003-ef9a-41d0-a749-0dcb235f336a" />

  这样，进入的就是定向的文件夹



### 3. Visual Studio Code 安装

1. 下载并安装 VS Code 

    [Visual Studio Code - The open source AI code editor | Your home for multi-agent development](https://code.visualstudio.com/) 地址

<img width="1014" height="677" alt="image" src="https://github.com/user-attachments/assets/641fbcf0-1d34-48d9-ae04-0f8b2a24cd1f" />

2. 安装核心插件：

    （1）Python

<img width="381" height="318" alt="image" src="https://github.com/user-attachments/assets/266f5526-a206-4f9d-a7f1-7927f0d3b734" />

    下同

    Jupyter

    Jupyter Keymap





 （2）在 VS Code 中新建 `.ipynb` 文件，验证 Jupyter 运行环境

  安装好选择内核（右上角），注意vs版本和jupyter版本之间是否冲突。


<img width="1148" height="891" alt="image" src="https://github.com/user-attachments/assets/4336215e-2b88-4325-9539-d83fa5bf03fe" />
