package com.example.myfirstkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composedemo.ui.theme.ComposeDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDemoTheme() {
               MyApp(modifier =  Modifier.fillMaxSize())

            }
        }
    }
}

@Composable
fun MyApp(modifier: Modifier = Modifier,names:List<String> = listOf("John","Jack"))
{
    Column(
        modifier = Modifier
    ){
        for (name in names){
            Greeting(name = name)
        }
    }
}


/**
 * 单个问候条目组件
 * 点击按钮可展开/收起底部额外内边距
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // 状态变量：控制按钮文本切换
    var mes by remember { mutableStateOf(true) }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 6.dp, horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(all = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello ",
                    color = Color.White
                )
                Text(
                    text = name,
                    color = Color.White
                )
            }
            ElevatedButton(
                onClick = { mes = !mes }
            ) {
                Text(if (mes) "change to one" else "change to two")
            }
        }
    }
}
/**
 * 预览函数：直接看到和截图一样的双条目效果
 */
@Preview(showBackground = true, name = "GreetingPreview")
@Composable
fun GreetingPreview() {
    ComposeDemoTheme() {
        MyApp()
    }
}