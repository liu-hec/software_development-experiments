

# Kotlin基础



任务完成截图




<img width="1139" height="663" alt="image" src="https://github.com/user-attachments/assets/85c564ea-cea8-4d67-ba70-cf3ef3be902e" />






<img width="1167" height="639" alt="image" src="https://github.com/user-attachments/assets/40d8444a-7b99-4424-95ab-06c72da9f3e2" />






<img width="1153" height="666" alt="image" src="https://github.com/user-attachments/assets/544fa21e-9ca3-477b-9c12-0592f15c4528" />




<img width="1170" height="657" alt="image" src="https://github.com/user-attachments/assets/05891651-3a1b-4e51-b52d-a515a855b0b7" />



<img width="1207" height="671" alt="image" src="https://github.com/user-attachments/assets/38ea4e45-e5f9-4055-883a-909d41752029" />



<img width="1172" height="587" alt="image" src="https://github.com/user-attachments/assets/7970aea6-bd87-4db1-99ec-2068f8625fc6" />




## 1 数据类型和变量

  main 程序入口  fun 表示函数标识   print/println同java

  可变变量 var  不可再赋值 =

  不可变变量/只读变量 val 



kotlin可以进行类型推断  var c =8     推断c为Int类型

**基本类型**  整数 Int Long Byte Short      val year: Int = 2020  

​     无符号整数  UByte  UShort   UInt   ULong     

​     布尔型 Boolean

​     浮点数 Float Double 

​    字符  Char

​    字符串 String       val message: String = "Hello, world!"

Any 类型 类似Java的Object 根类型



支持算术运算 + - * /  和逻辑运算  ！> xor(异或-fun函数) > &&  > || 



**类型转换**

 没有隐形转换，需要通过显式类型转换，调用对应的转换函数

 

```
val I : Int = 10 
    val i :Double = I.toDouble()
    println(i) //10.0
     
    val a : Double =1 // Initializer type mismatch: expected 'Double', actual 'Int'. 不支持隐形转换
    
```

   转换运算符as

as 不安全转换

```
val obj: Any = "hello"
val str: String = obj as String  // 正常
val num: Int = obj as Int        // 抛出 ClassCastException 失败
```



as?安全转换  返回**可以为空的值**   因此变量要可以为空 ？=

```
val obj: Any = 123
val str: String? = obj as? String  // 转换失败，str = null 失败不抛出异常 return null
val int: Int? = obj as? Int        // 转换成功，int = 123
```



is 类型检查运算符  ! is

```
 val obj : Any = "fef"
if (obj is String) {
    println(obj.length) // 自动智能转换为 String
}//一旦 is 检查通过，编译器会自动把对象转换成目标类型，不用写强制转换代码

if (obj !is Int) {
    println("不是 Int 类型")
}
```








​    







## 2.集合

**List** 

有序的可重复的集合  只读 **listOf**（）创建List   可变：mutableListOf（）创建MutableList

  这里可变是指：元素可以变化 可以增add删remove改查等

访问：[ ] 或者函数 第一个first()  最后一个 last()

```
val readOnlyShapes = listOf("triangle", "square", "circle")
println("the first one in list is  ${readOnlyShapes[0]}")//the first one in list is  triangle
println("the first one in list is  ${readOnlyShapes.first()}")//the first one in list is  triangle
println("the last one in list is  ${readOnlyShapes.last()}")//the last one in list is  circle

```

还有 count()-元素个数   in 判断是否在list内   增加 add()   删除remove()

一般情况无需写类型 例如 ：List<String> 会自动判断类型





**Set**

无序的唯一元素的集合  

要创建只读集（[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/)），可以使用 [`setOf（）`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/set-of.html) 函数。

要创建可变集（[`MutableSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/)），可以使用 [`mutableSetOf（）`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-set-of.html) 函数。

类比List 同理   但是没有[ ]访问符（无序 无法直接get根据下标）

如何访问：遍历访问 for(item in set ){    }  ;迭代器访问；转为List访问（toList()）等

 

```
val set = setOf("A","B")
val iterator = set.iterator()
while (iterator.hasNext()) {
    println(iterator.next())
}
```



**Map**

 键值对  键唯一 值可以重复

要创建只读映射（[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/)），可以使用 [`mapOf（）`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map-of.html) 函数。

要创建可变映射（[`MutableMap`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/)），可以使用 [`mutableMapOf（）`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/mutable-map-of.html) 函数。 声明同理 一般可以自动推断

```
// Mutable map with explicit type declaration
val juiceMenu: MutableMap<String, Int> = mutableMapOf("apple" to 100, "kiwi" to 190, "orange" to 100)
juiceMenu["pig"] = 250
println(juiceMenu)
// {apple=100, kiwi=190, orange=100,pig =250}  //[键]来访问或者新增
```

 访问失败例如找不到对应键 返回null

```
juiceMenu.remove("orange")    //count containsKey("orange")
```

分别获取键和值  使用in判断键或者值是否存在在map中

```
println(readOnlyJuiceMenu.keys)
// [apple, kiwi, orange]
println(readOnlyJuiceMenu.values)
// [100, 190, 100]

println(200 in readOnlyJuiceMenu.values)//false
```





## **3.控制流程**



### **条件分支**

 **if-else 分支**

​      if(){   }  else if {} ...... 形式解决

kotlin没有三元表达式  但是可以这样表示：condition ? then : else``if``{}  

Kotlin 的 `if` 是**表达式（有返回值）**，不是语句，所以能直接赋值、替代三元运算符

```
   val age = 20
   val type = if (age >= 18) "成年" else "未成年" 
    println(type)
// 等价 Java：String type = age >=18 ? "成年" : "未成年";
    
    
```





**when 分支**

 格式  when(obj){

​     验证1 -> 验证成功的动作.....

   

  }

   ---执行满足条件的第一个分支

```
val obj = "1"

when (obj) {
    "1" -> if(obj is String) println("obj is String")
    "Hello" -> println("Greeting")
    // Default statement
    else -> println("Unknown")     //obj is String
}
```

还可以用变量来承接when的返回值



另一种表达：

```
 val trafficLightState = "Red" // This can be "Green", "Yellow", or "Red"

    val trafficAction = when {
        trafficLightState == "Green" -> "Go"
        trafficLightState == "Yellow" -> "Slow down"
        trafficLightState == "Red" -> "Stop"
        else -> "Malfunction"
    }

    println(trafficAction)
```



### **循环**

范围range： a..b表示a到b（包含）  1..4 --1 2 3 4      逆序 b downTo  a  例如 4 downTo 1--4  3 2 1  

​              不包含end a..<b  例如 1..<4--1 2 3

​     对于Char 也是可以的 例如  ‘a'..'d' 表示 ’a‘  ‘b’  ‘c’  ‘d’ 



**for循环**

 for(临时变量 in range)  range还可以是集合

```
 for(num in 2 downTo 1){
        println(num)
   }// 2 1


val cakes = listOf("carrot", "cheese", "chocolate")

for (cake in cakes) {
    println("Yummy, it's a $cake cake!")
}
```





**while循环**

- 条件表达式为真时执行代码块。(`while`)
- 先执行代码块，然后检查条件表达式。(`do-while`)

```
var cakesEaten = 0
var cakesBaked = 0
while (cakesEaten < 3) {
    println("Eat a cake")
    cakesEaten++
}
do {
    println("Bake a cake")
    cakesBaked++
} while (cakesBaked < cakesEaten)
// Eat a cake
// Eat a cake
// Eat a cake
// Bake a cake
// Bake a cake
// Bake a cake
```



运用  使用when + is代替if-else   switch类似

```
fun handleData(obj: Any) = when (obj) {
    is String -> "字符串：${obj.length}"  // 智能转换
    is Int -> "整数：${obj * 2}"          // 智能转换
    is Boolean -> "布尔值：${!obj}"
    else -> "其他类型"
}
```





## 4.函数

**fun关键字 函数名（参数情况）：返回值类型**{

​          函数体

   return sth

}

函数名：小驼峰 首字符小写 其余满足驼峰命名

```
fun sum(x: Int, y: Int): Int {
    return x + y
}
调用可以不包含参数名
println(sum(10,20))
println(x = 10,y = 20)
```



默认参数值，调用可以忽略，也可以覆盖

```
fun printMessageWithPrefix(message: String, prefix: String = "Info") {
    println("[$prefix] $message")
}

fun main() {
    // Function called with both parameters
    printMessageWithPrefix("Hello", "Log") 
    // [Log] Hello
    
    // Function called only with message parameter 默认参数值
    printMessageWithPrefix("Hello")        
    // [Info] Hello
    
    printMessageWithPrefix(prefix = "Log", message = "Hello")  显式指明传入参数对应的值
    // [Log] Hello
}
```

你可以跳过默认值的参数，而不是全部省略。然而，在第一个跳过的参数之后，你必须命名所有后续参数 参数名 = value。

```
fun printMessageWithPrefix(message: String, prefix: String = "Info",p:Int = 20) {
    println("[$prefix] $message p = $p")
}

fun main() {
    // Function called with both parameters
    printMessageWithPrefix("Hello",10) //跳过prefix 必须全命名参数名 = value 
    // [Log] Hello
}
```

**无返回 无需返回类型 无需return**  可以省略return Unit(类似Java的void 但是**Unit是一个单例类 可初始化赋值**等)

fun s( ) : Unit {   return Unit is optional}



单表达式函数简化：

```
fun sum(x: Int, y: Int) = x + y  //直接 = 
```







**lambda表达式**

格式：

函数名：（参数类型...）->返回参数类型 =   {参数1：类型，参数2：类型.... ->函数体/返回值 }            参数->函数体

```
val sum = {x:Int,y:Int -> x+y}
println(sum(10,20))//调用

() -> Unit 无参数 返回空
```



（1）类型省略 

val **sum**: **(Int, Int) -> Int = { a, b -> a + b }**完整形式 这里可以省略Int的类型 kotlin会自动进行类型推导

  (2) **单参数使用it**指代

```
 val upperCaseString:(String) -> String = { it -> it.uppercase() }
    println(upperCaseString("hello"))
    // HELLO
```

 （3）lambda表达式作为参数 

​      例如集合的forEach,filter，map等等

```
val numbers = listOf(1, -2, 3, -4, 5, -6)

val positives = numbers.filter ({ x -> x > 0 })
println(positives)
// [1, 3, 5]

numbers.forEach { println(it) }  //单参数 it指代

// 转换：map（每个元素乘2）
val doubled = list.map { it * 2 }
```

（4）单独调用

```
println({ text: String -> text.uppercase() }("hello"))
// HELLO
```









## 5.类

class  默认继承**根类Any**(具有equals , hashCode,toString三个基础方法)

   格式：class name {

​              类体，属性方法 初始化等   

​        }

  主体式可选的

类的属性声明还可以在（）内

```
class Contact(val id: Int = 10 , var email: String = "fef"){
    
    fun getid(): Int {
        return id+10
    }
}

class ContactT(){
    val id:Int =10  //	必须显式赋值或标记为抽象
    var email:String = "fef"
}

fun main() {
   val c = Contact()
   println(c.id )//10 使用默认值 相当于java的c.getId()
   c.email="updated@email"
   println("修改后的id ${c.email}  or  "+c.email) //打印String    $c 解析c这个类，获取地址 ${}整体解析 
   
   val cc =ContactT()
   println(c.id) // 10 属性必须初始化或者抽象类内未抽象属性
   
   println(c)//Contact@1ddc4ec2 地址
   println(cc) //ContactT@133314b 地址
   
```

 property 属性必须初始化或者抽象



特点：不可继承 final,除非加上open



同理：在（）内声明私有属性，默认创建对应的构造函数，set/get函数，{ }声明类似 但是必须初始化赋值或者抽象类





**创建实例**

   构造函数声明一个类实例

```
val contact = Contact(1, "mary@gmail.com") //不传入使用默认值
```



访问属性

  变量.属性名 （= 赋值）  使用字符串模板${  }连接输出

```kotlin
println("Their email address is: ${contact.email}")
```

还可以自行创建成员函数等



**数据类**data

```
data class Contact(val id: Int = 10 , var email: String = "fef")
```

针对**主构造函数里的属性**，自动生成 5 个核心方法：

1. **`toString()`** → 打印对象**显示属性值**，不再打印乱码地址（解决你之前的痛点！）

2. **`equals()` / `hashCode()`** → 比较两个对象**属性是否相等** ，不是比较内存地址

3. **`copy()`** → 快速复制对象，可修改部分属性 例如

    ```
    val c3 = c1.copy(email = "new@123.com")
    ```

4. **`componentN()`** → 解构对象（快速拆包）





Nothing类 是所有类的子类型 bottom class

Nothing 是 `final` 类，构造函数是**私有**的，**永远无法创建它的对象**，没有任何实例

作用：标记**永远不会返回值**的代码 / 函数，或**永远不可达**的代码分支

```
// 抛异常，永远不返回，返回类型 Nothing
fun error(message: String): Nothing {
    throw IllegalArgumentException(message)
}

```



**继承extends**





**接口 interface**

   可以包含**抽象方法**（无实现）和**默认方法**（有实现）

   可以声明**抽象属性**（无初始值）和**带访问器的属性**（有默认实现）

   一个类可以**实现多个接口**（多继承）

   接口不能有构造函数，不能持有状态（不能有带 backing field 的属性）

   所有成员默认是 `public` 的

```
interface Clickable {
    // 抽象方法：必须由实现类重写
    fun onClick()
    
    // 默认方法：有默认实现，实现类可选择性重写
    fun onLongClick() {
        println("默认长按事件")
    }
    
    // 抽象属性：必须由实现类提供
    val clickCount: Int
    
    // 带访问器的属性：有默认实现
    val isClickable: Boolean
        get() = clickCount > 0
}
```

  接口继承接口  多继承

```
interface Interactive : Clickable, Focusable
```





## 6.空安全

  编译时检测是否为空 而非运行时  更加安全调用

**可空类型**   类型？

默认情况下，**类型不允许接受值。**可空类型通过在类型声明后添加显式添加来声明

Kotlin 中**所有类型默认都是非空的**，绝对不能赋值为 `null`，编译器会直接报错

    var nullable: String? = "You can keep a null here" 
    
        // This is OK
        nullable = null

​     可空类型的变量**不能直接调用方法或访问属性 ** null.func()??? 



函数如何传入一个可以为空的参数呢？

 describeString(maybeString: String?)  加上？表示允许String 为null的值传入作为参数



**空值判断**

可以使用 == null判断

可以使用安全调用操作符 ？.  可以串联调用 **a?.b?. **      等等

```
fun lengthString(maybeString: String?): Int? = maybeString?.length   传入null 返回null
```

 可以使用 E**lvis 操作符 `?:`**

​                  如果左边表达式为 null，就返回**右边的值**；否则返回左边的值  null右边非null左边

```
 val nullString: String? = null
    println(nullString?.length ?: 0)
    // 0
```



非空断言操作符 `!!`（慎用！）

**强制告诉编译器：这个对象绝对不为 null**。如果对象实际上为 null，会立即抛出 `NullPointerException`。

```
  val email: String? = "test@123.com"
      println(email!!.length) // ✅ 输出：12

      val nullEmail: String? = null
      println(nullEmail!!.length) // ❌ 运行时崩溃：NullPointerException

```

