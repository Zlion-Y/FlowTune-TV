package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.flowtune.tv.AppGraph
import com.flowtune.tv.online.SourceInfo
import java.io.File

private val Accent = Color(0xFF4F8CFF)

/** 音源管理：扫描 /sdcard 下的 .js LX 音源脚本，导入 / 启用 / 删除。 */
@Composable
fun SourceManager(state: AppState) {
    val sources by AppGraph.online.sources.collectAsState()
    val active by AppGraph.online.activeSource.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var candidates by remember { mutableStateOf<List<File>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        candidates = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AppGraph.online.scanSourceFiles(context)
        }
    }

    Column {
        if (sources.isEmpty()) {
            Text("未导入音源（网易云免费曲目仍可直接播放）", color = Color(0xFF88888E), fontSize = 13.sp)
        } else {
            sources.forEach { src ->
                val isActive = active?.info == src.info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isActive) Color(0xFF1F3A5F) else Color(0xFF1F1F23),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${src.info.name} v${src.info.version}", color = Color.White, fontSize = 14.sp)
                        if (src.info.author.isNotBlank()) {
                            Text("by ${src.info.author}", color = Color(0xFF88888E), fontSize = 11.sp)
                        }
                    }
                    if (!isActive) {
                        Button(
                            onClick = { AppGraph.online.setActive(src.info) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E33), contentColor = Color.White),
                            modifier = Modifier.height(34.dp)
                        ) { Text("启用", fontSize = 12.sp) }
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Text("使用中", color = Accent, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = { AppGraph.online.removeSource(src.info) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A2325), contentColor = Color(0xFFFF8B8B)),
                        modifier = Modifier.height(34.dp)
                    ) { Text("删除", fontSize = 12.sp) }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (candidates.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("可导入：", color = Color(0xFF88888E), fontSize = 12.sp)
            candidates.forEach { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                message = "导入中：${file.name}"
                                val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    AppGraph.online.importSource(file)
                                }
                                val imported = r.getOrNull()
                                message = if (imported != null) "已导入：" + imported.name
                                          else "导入失败：" + (r.exceptionOrNull()?.message ?: "")
                                candidates = candidates - file
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("＋ ${file.name}", color = Accent, fontSize = 13.sp, maxLines = 1)
                }
            }
        } else if (sources.isEmpty()) {
            Text("未在 /sdcard/Download 发现 .js 音源脚本", color = Color(0xFF6E6E74), fontSize = 12.sp)
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(message, color = Accent, fontSize = 12.sp)
        }
    }
}
