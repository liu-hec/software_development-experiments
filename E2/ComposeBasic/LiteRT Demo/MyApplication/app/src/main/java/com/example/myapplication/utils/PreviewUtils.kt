package com.example.myapplication.utils


import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme

// ⭐ 所有 Preview 都包这个
@Composable
fun PreviewContainer(content: @Composable () -> Unit) {
    MyApplicationTheme() {
        Surface {
            content()
        }
    }
}