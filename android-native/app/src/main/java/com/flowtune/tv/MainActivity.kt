package com.flowtune.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import com.flowtune.tv.ui.FlowTuneApp
import com.flowtune.tv.ui.rememberAppState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 隐藏系统 UI（全屏沉浸）
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            FlowTuneRoot()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // 媒体键走 MediaSession；其余按键 Compose 焦点系统天然处理
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
private fun FlowTuneRoot() {
    val state = rememberAppState()
    FlowTuneApp(state)
}
