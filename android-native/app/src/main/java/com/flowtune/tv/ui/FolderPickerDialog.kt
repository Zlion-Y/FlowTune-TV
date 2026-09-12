package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 原生文件夹选择对话框：D-pad 浏览 /storage 根与内部存储，选中当前目录确认。
 */
@Composable
fun FolderPickerDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf<File?>(null) }  // null = 存储根
    var children by remember { mutableStateOf<List<File>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    suspend fun listDir(dir: File?) = withContext(Dispatchers.IO) {
        if (dir == null) {
            val roots = mutableListOf<File>()
            val internal = File("/storage/emulated/0")
            if (internal.exists()) roots.add(internal)
            roots
        } else {
            dir.listFiles()
                ?.filter { it.isDirectory && it.name != "Android" }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        }
    }

    LaunchedEffect(current) {
        loading = true
        children = listDir(current)
        loading = false
    }

    BackHandler(enabled = current != null) {
        current = current!!.parentFile?.takeIf { it.absolutePath != "/" && it.absolutePath != "/storage" }
    }
    BackHandler(enabled = current == null) { onDismiss() }
    val dialogFocus = remember { FocusRequester() }
    val firstRowFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        runCatching { dialogFocus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .focusRequester(dialogFocus)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(640.dp)
                .height(520.dp)
                .background(Color(0xFF232327), RoundedCornerShape(14.dp))
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                Text("选择文件夹", color = Color.White, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(current?.absolutePath ?: "存储设备", color = Color(0xFF88888E), fontSize = 11.sp, maxLines = 1)
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                if (loading) {
                    item { Text("读取中…", color = Color(0xFF88888E), fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
                } else {
                    if (current == null) {
                        children.forEachIndexed { idx, root ->
                            item(key = root.absolutePath) {
                                PickerRow("▸ " + root.absolutePath, focusRequester = if (idx == 0) firstRowFocus else null) { current = root }
                            }
                        }
                    } else {
                        if (children.isEmpty()) {
                            item { Text("此文件夹为空", color = Color(0xFF6E6E74), fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
                        }
                        itemsIndexed(children, key = { _: Int, f: File -> f.absolutePath }) { idx: Int, dir: File ->
                            PickerRow("▸ " + dir.name, focusRequester = if (idx == 0) firstRowFocus else null) { current = dir }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E33), contentColor = Color(0xFFEDEDEF))
                ) { Text("取消") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { val c = current; if (c != null) onPick(c.absolutePath) else onDismiss() },
                    enabled = current != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F8CFF), contentColor = Color.White)
                ) { Text("选择此文件夹") }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        label,
        color = if (focused) Color.White else Color(0xFFCFCFD4),
        fontSize = 14.sp,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(8.dp))
            .tvFocusGlow(focused, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
