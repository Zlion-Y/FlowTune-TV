package com.flowtune.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.flowtune.tv.AppGraph
import com.flowtune.tv.online.SourceInfo
import java.io.File

private val Accent = Color(0xFF4F8CFF)

/** 音源管理：扫描 /sdcard 下的 .js LX 音源脚本 + URL 导入，导入 / 启用 / 删除。 */
@Composable
fun SourceManager(state: AppState, firstFocus: FocusRequester? = null) {
    val sources by AppGraph.online.sources.collectAsState()
    val active by AppGraph.online.activeSource.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<File>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        candidates = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AppGraph.online.scanSourceFiles(context)
        }
    }
    // 局域网上传的音源自动导入
    LaunchedEffect(Unit) {
        com.flowtune.tv.online.LanSourceServer.uploads.collect { file ->
            message = "收到上传：${file.name}，导入中…"
            val imp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                AppGraph.online.importSource(file)
            }
            message = if (imp.getOrNull() != null) "已导入：" + imp.getOrThrow().name
                      else "导入失败：" + (imp.exceptionOrNull()?.message ?: "")
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
                        TvButton("启用", verticalPadding = 8.dp) { AppGraph.online.setActive(src.info) }
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Text("使用中", color = Accent, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    TvButton("删除", container = Color(0xFF3A2325), contentColor = Color(0xFFFF8B8B), verticalPadding = 8.dp) {
                        AppGraph.online.removeSource(src.info)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (candidates.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("可导入：", color = Color(0xFF88888E), fontSize = 12.sp)
            candidates.forEachIndexed { idx, file ->
                var focused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (idx == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .background(Color.Transparent, TvShape)
                        .tvFocusGlow(focused, TvShape)
                        .clickable {
                            scope.launch {
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
                    Text("＋ ${file.name}", color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
            }
        } else if (sources.isEmpty()) {
            Text("未在 /sdcard/Download 发现 .js 音源脚本", color = Color(0xFF6E6E74), fontSize = 12.sp)
        }

        // URL 音源导入
        Spacer(Modifier.height(12.dp))
        Text("从 URL 导入音源脚本（.js 链接）", color = Color(0xFF88888E), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        var urlFieldFocused by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(Color(0xFF1F1F23), TvShape)
                    .tvFocusGlow(urlFieldFocused, TvShape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFEDEDEF), fontSize = 13.sp),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { urlFieldFocused = it.isFocused }
                )
                if (urlInput.isEmpty()) {
                    Text("https://…/source.js", color = Color(0xFF6E6E74), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            TvButton(
                "导入",
                container = Accent,
                verticalPadding = 11.dp,
            ) {
                val url = urlInput.trim()
                if (!url.startsWith("http")) {
                    message = "请输入有效的 http(s) 链接"
                } else {
                    scope.launch {
                        message = "下载中…"
                        val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                val resp = com.flowtune.tv.online.Platforms.http
                                    .newCall(okhttp3.Request.Builder().url(url).build())
                                    .execute()
                                resp.use { r2 ->
                                    check(r2.isSuccessful) { "HTTP ${r2.code}" }
                                    val script = r2.body!!.string()
                                    val f = File(context.cacheDir, "lx-url-${System.currentTimeMillis()}.js")
                                    f.writeText(script)
                                    f
                                }
                            }
                        }
                        val file = r.getOrNull()
                        if (file == null) {
                            message = "下载失败：" + (r.exceptionOrNull()?.message ?: "")
                        } else {
                            message = "导入中：${file.name}"
                            val imp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                AppGraph.online.importSource(file)
                            }
                            message = if (imp.getOrNull() != null) "已导入：" + imp.getOrThrow().name
                                      else "导入失败：" + (imp.exceptionOrNull()?.message ?: "")
                            if (imp.getOrNull() != null) urlInput = ""
                        }
                    }
                }
            }
        }

        // 局域网导入：手机/电脑浏览器上传，扫码直达
        Spacer(Modifier.height(16.dp))
        Text("局域网导入（手机/电脑上传音源）", color = Color(0xFF88888E), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        var lanUrl by remember { mutableStateOf<String?>(null) }
        var qr by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvButton(
                if (lanUrl != null) "停止服务" else "启动服务",
                container = if (lanUrl != null) Color(0xFF3A2325) else Accent,
                verticalPadding = 10.dp,
                onClick = {
                    if (lanUrl != null) {
                        com.flowtune.tv.online.LanSourceServer.stop()
                        lanUrl = null; qr = null
                        message = "局域网导入服务已停止"
                    } else {
                        lanUrl = com.flowtune.tv.online.LanSourceServer.start(context.cacheDir) { message = it }
                        lanUrl?.let { qr = com.flowtune.tv.online.LanSourceServer.qrBitmap(it) }
                    }
                },
            )
            Text(
                if (lanUrl != null) "服务运行中，上传后自动导入" else "启动后手机扫码或浏览器访问即可上传 .js 音源",
                color = Color(0xFF88888E), fontSize = 11.sp
            )
        }
        lanUrl?.let { url ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                qr?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "上传地址二维码",
                        modifier = Modifier
                            .size(150.dp)
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(url, color = Accent, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("手机扫码打开上传页，选择 .js 文件上传后自动导入", color = Color(0xFF88888E), fontSize = 11.sp)
                }
            }
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(message, color = Accent, fontSize = 12.sp)
        }
    }
}
