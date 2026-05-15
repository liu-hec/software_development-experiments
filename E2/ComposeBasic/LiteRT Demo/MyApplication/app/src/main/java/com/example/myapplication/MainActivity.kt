package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.CameraTheme
import com.example.myapplication.ui.theme.MyApplicationTheme
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// ================= MainActivity =================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 允许内容延伸到状态栏
        enableEdgeToEdge()   // ⭐ 取代 statusBarColor

        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false   // false = 白色图标

        setContent {
            // 稳定版主题，无实验性API
           MyApplicationTheme() {
                LiteRTApp()
               val density = resources.displayMetrics.density
               val widthDp = resources.displayMetrics.widthPixels / density
               val heightDp = resources.displayMetrics.heightPixels / density

               Log.d("SCREEN", "widthDp=$widthDp heightDp=$heightDp")
            }
        }
    }
}

// 主界面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteRTApp() {
    // 状态变量（初始化保证显示正常）
    var modelName by remember { mutableStateOf("MobileNet") }
    var result by remember { mutableStateOf("Cat") }
    var confidence by remember { mutableStateOf("96.2%") }
    var time by remember { mutableStateOf("28 ms") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri   // 选中的图片返回这里
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1565C0)),

        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LiteRT AI Demo")},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Blue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.Unspecified
                ) //lambda
            )
        }
    ) { paddingValues ->   // 👈 关键：避免被状态栏遮挡

        Column(
            modifier = Modifier
                .padding(paddingValues)   // 👈 必须加
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 相机预览区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray, RoundedCornerShape(12.dp))
                        ,
                    contentAlignment = Alignment.Center
                ) {

                    if (imageUri == null) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                           horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "camera",
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Text("Camera Preview", color = Color.DarkGray, fontSize = 20.sp)

                        }
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 结果卡片（变量正常显示）
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("模型: $modelName", fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("结果: $result", fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("置信度: $confidence", fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("耗时: $time", fontSize = 18.sp, color = Color.Black)
                    }
                }

                // 第一行按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 拍照识别
                    Button(
                        onClick = {
                            result = "拍照识别完成"
                            confidence = "99%"
                            time = "30 ms"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {

                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("拍照识别", color = Color(Color.White.hashCode()) , fontSize = 16.sp)
                    }

                    // 相册导入
                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")   // 打开相册
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))

                        Text("相册导入", color = Color.White, fontSize = 16.sp)
                    }
                }

                // 第二行按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 切换模型
                    Button(
                        onClick = {
                            modelName = if (modelName == "MobileNet") "ResNet" else "MobileNet"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("切换模型", color = Color.White, fontSize = 16.sp)
                    }

                    // 清空结果
                    Button(
                        onClick = {
                            modelName = ""
                            result = ""
                            confidence = ""
                            time = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ){
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("清空结果", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


@Preview(
    device = "id:pixel_7",
    showSystemUi = true,
    widthDp = 411,
    heightDp = 891
)
@Composable
fun PCamera(){
    MyApplicationTheme()
     {
        LiteRTApp()
    }
}
