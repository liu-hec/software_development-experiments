

# 1.selection_sort的实现 

1.掌握py的基本语法，使用del定义函数，基本的数据类型和程序控制的三种-顺序，分支和循环，使用冒泡排序完成selection_sort的实现

```python
def selection_sort(arr):
    #判断是否是数字arr
    if not all(isinstance(i, (int, float)) for i in arr):
        raise ValueError("输入的数组必须是数字类型")
    n =len(arr)
    for i in range(n-1): #生成0-n-2的indexs
        for j in range(i+1,n):
             if(arr[i] > arr[j]):
                 arr[i], arr[j] = arr[j], arr[i] #交换

    return arr

arr = [64, 25, 12, 22, 11,"c"]

def test_selection_sort():
    arr = [64, 25, 12, 22, 11]
    sorted_arr = selection_sort(arr)
    flag = sorted_arr == [11, 12, 22, 25, 64], f"Expected [11, 12, 22, 25, 64], but got {sorted_arr}"
    return flag

print(test_selection_sort())#true

```

    (True, 'Expected [11, 12, 22, 25, 64], but got [11, 12, 22, 25, 64]')





# 2.matplotlib 数据分析入门

##    2.1  依赖下载

   首先对于依赖，在conda中下载对应的依赖，

```
conda install seaborn -y
conda install numpy -y等等
```

## 2.数据导入、数据预处理和图像绘制

   这里获取的csv文件，包含25000条数据，使用pd的read_csv，获取的是DataFrame，类似excel表格，带有行和列。

​     df.head()和df.tail()分别获取前5行和后5行的数据。

​      如何进行profit的数据处理：类型的转换，将N.A.数据去除（这类数据占比少，影响小）

​    （1）df.dtypes可以发现，profit的类型是str类型，因此使用正则找出不符合数字类型的profit所在的行。

```
non_numberic_profits = df.profit.str.contains('[^0-9.-]') profit的str类型不包含数字/./-
```

​      （2）去除对应的行，df.profit = df.profit.apply(pd.to_numeric) 将类型转为数字类型

```python
%matplotlib inline
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os 

print(os.getcwd())  # getcwd 的意思是 Get Current Working Directory（获取当前工作目录）
df = pd.read_csv('fortune500.csv') 
      #是pandas常用的数据结构，称为DataFrame，可以理解为数据表
print(df.head()) #打印前面五行数据 

print(df.tail()) #打印后面五行数据 0-24999
df.columns = ['year', 'rank', 'company', 'revenue', 'profit']
print("数据长度  "+str(len(df))) #25500

print(df.dtypes)


#将profit数据类型进行切换
non_numberic_profits = df.profit.str.contains('[^0-9.-]') #^  返回布尔序列 true-不为数字的profit行号
df.loc[non_numberic_profits].head() #N.A. 不符合的的数据行

len(df.profit[non_numberic_profits]) #profit不符合的数据长度 369


bin_sizes, _, _ = plt.hist(df.year[non_numberic_profits], bins=range(1955, 2006))
#删除profit不符合的数据行
df = df.loc[~non_numberic_profits]
df.profit = df.profit.apply(pd.to_numeric)
print(len(df)) #25131


group_by_year = df.loc[:, ['year', 'revenue', 'profit']].groupby('year')
avgs = group_by_year.mean()
x = avgs.index
y1 = avgs.profit
def plot(x, y, ax, title, y_label):
    ax.set_title(title)
    ax.set_ylabel(y_label)
    ax.plot(x, y)
    ax.margins(x=0, y=0)

fig, ax = plt.subplots()
plot(x, y1, ax, 'Increase in mean Fortune 500 company profits from 1955 to 2005', 'Profit (millions)')


y2 = avgs.revenue
fig, ax = plt.subplots()
plot(x, y2, ax, 'Increase in mean Fortune 500 company revenues from 1955 to 2005', 'Revenue (millions)')


def plot_with_std(x, y, stds, ax, title, y_label):
    ax.fill_between(x, y - stds, y + stds, alpha=0.2)
    plot(x, y, ax, title, y_label)
fig, (ax1, ax2) = plt.subplots(ncols=2)
title = 'Increase in mean and std Fortune 500 company %s from 1955 to 2005'
stds1 = group_by_year.std().profit.values
stds2 = group_by_year.std().revenue.values
plot_with_std(x, y1.values, stds1, ax1, title % 'profits', 'Profit (millions)')
plot_with_std(x, y2.values, stds2, ax2, title % 'revenues', 'Revenue (millions)')
fig.set_size_inches(14, 4)
fig.tight_layout()

```

    d:\claudeProjects\JupyterTest
       Year  Rank           Company  Revenue (in millions) Profit (in millions)
    0  1955     1    General Motors                 9823.5                  806
    1  1955     2       Exxon Mobil                 5661.4                584.8
    2  1955     3        U.S. Steel                 3250.4                195.4
    3  1955     4  General Electric                 2959.1                212.6
    4  1955     5            Esmark                 2510.8                 19.1
           Year  Rank                Company  Revenue (in millions)  \
    25495  2005   496        Wm. Wrigley Jr.                 3648.6   
    25496  2005   497         Peabody Energy                 3631.6   
    25497  2005   498  Wendy's International                 3630.4   
    25498  2005   499     Kindred Healthcare                 3616.6   
    25499  2005   500   Cincinnati Financial                 3614.0   
    
          Profit (in millions)  
    25495                  493  
    25496                175.4  
    25497                 57.8  
    25498                 70.6  
    25499                  584  
    数据长度  25500
    year         int64
    rank         int64
    company        str
    revenue    float64
    profit         str
    dtype: object
    25131




<img width="635" height="462" alt="image" src="https://github.com/user-attachments/assets/3279aab8-aa89-4b4a-a293-dcad4913fd44" />
    




<img width="687" height="476" alt="image" src="https://github.com/user-attachments/assets/50f994e7-99a4-4f47-a881-60e44ec023f6" />
    




<img width="677" height="482" alt="image" src="https://github.com/user-attachments/assets/4f73add5-5cb6-4c28-9337-5e8f3f1ea25a" />
    




<img width="1203" height="388" alt="image" src="https://github.com/user-attachments/assets/d77b3212-bc54-4782-a52d-0882de498676" />
    

​        如何使用matplotlib画图？

   

```
# 把数据按 'year'（年份）分组，就像把学生按班级分好。
group_by_year = df.loc[:, ['year', 'revenue', 'profit']].groupby('year')

# 对分好的每一组算平均值 (mean)。
avgs = group_by_year.mean()

# 准备画图用的 X 轴和 Y 轴数据
x = avgs.index # X轴：年份
y1 = avgs.profit # Y轴1：平均利润
y2 = avgs.revenue # Y轴2：平均营收

```

  定义完画图函数，具体根据实际需求来构成即可。

```
# 定义一个叫 plot 的画图小帮手
def plot(x, y, ax, title, y_label):
    ax.set_title(title) # 设置图表标题
    ax.set_ylabel(y_label) # 设置 Y 轴名字
    ax.plot(x, y) # 画线！
    ax.margins(x=0, y=0) # 去掉图表边缘的白边

# 画第一张图：利润增长图
fig, ax = plt.subplots() # 拿出一张画布 (fig) 和一个画框 (ax)
plot(x, y1, ax, '标题略...', 'Profit (millions)')

# 画第二张图：营收增长图
fig, ax = plt.subplots()
plot(x, y2, ax, '标题略...', 'Revenue (millions)')
```







```python
# 将利润和收入同时绘制在一张图上
# 使用双 Y 轴处理利润和收入量级不同的问题
fig, ax1 = plt.subplots(figsize=(10, 6))

color = 'tab:blue'
ax1.set_xlabel('Year')
ax1.set_ylabel('Profit (millions)', color=color)
ax1.plot(x, y1, color=color, label='Profit')
ax1.tick_params(axis='y', labelcolor=color)

# 实例化共享相同 x 轴的第二个 axes
ax2 = ax1.twinx()  
color = 'tab:orange'
ax2.set_ylabel('Revenue (millions)', color=color)  
ax2.plot(x, y2, color=color, label='Revenue')
ax2.tick_params(axis='y', labelcolor=color)

fig.tight_layout()  
plt.title('Increase in mean Fortune 500 company profits and revenues from 1955 to 2005')
plt.show()
```


<img width="1037" height="615" alt="image" src="https://github.com/user-attachments/assets/4f28302c-4f5c-41f7-8978-ee2418b1e606" />
    



  **同一图双 Y 轴**: 使用 `ax2 = ax1.twinx()` 在同一 `x` 下绘制 `profit`（左轴）和 `revenue`（右轴），分别用不同颜色区分，便于比较不同量级时间序列变化。当然也可以画在同一个y轴内。

```
fig, ax = plt.subplots(figsize=(10,6))

ax.set_xlabel('Year')
ax.set_ylabel('Millions')

ax.plot(x, y1, label='Profit')    # 利润
ax.plot(x, y2, label='Revenue')   # 收入

ax.legend()   # 显示图例
plt.title('Profit vs Revenue')
plt.show()


```

<img width="1051" height="602" alt="image" src="https://github.com/user-attachments/assets/f20da8b9-bf9a-46ae-9638-2dafc3843d43" />
