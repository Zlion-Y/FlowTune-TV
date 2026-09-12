package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.model.Playlist

private val BgSelected = Color(0xFF2E2E33)
private val Accent = Color(0xFF4F8CFF)

@Composable
fun Sidebar(
    playlists: List<Playlist>,
    selectedId: String,
    onlineTab: String?,
    onSelect: (String) -> Unit,
    onOpenOnline: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSponsor: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        firstFocus.requestFocus()
    }
    Column(
        modifier
            .background(Color(0xFF1B1B1F))
            .padding(vertical = 24.dp)
    ) {
        Text(
            "在线音乐",
            color = Color(0xFF88888E),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        NavItem("搜索", icon = Icons.Filled.LibraryMusic, active = onlineTab == "search", focusRequester = firstFocus) { onOpenOnline("search") }
        NavItem("排行榜", icon = Icons.Filled.LibraryMusic, active = onlineTab == "charts") { onOpenOnline("charts") }
        NavItem("歌单", icon = Icons.Filled.LibraryMusic, active = onlineTab == "playlists") { onOpenOnline("playlists") }

        Spacer(Modifier.height(24.dp))
        Text(
            "歌单",
            color = Color(0xFF88888E),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        playlists.forEach { pl ->
            val selected = pl.id == selectedId
            var rowFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .background(
                        if (selected) BgSelected else Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    )
                    .tvFocusGlow(rowFocused, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .onFocusChanged { rowFocused = it.isFocused }
                    .clickable { onSelect(pl.id) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (pl.id == "favorites") Color(0xFFE8536A) else Color(0xFF9A9AA0),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(pl.name, color = Color(0xFFEDEDEF), fontSize = 15.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        NavItem("赞助作者", icon = Icons.Filled.LibraryMusic, onClick = onOpenSponsor)
        NavItem("设置", icon = Icons.Filled.Settings, onClick = onOpenSettings)
    }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean = false, focusRequester: FocusRequester? = null, onClick: (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(
                if (active) BgSelected else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            )
            .tvFocusGlow(focused, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .let { m -> if (onClick != null) m.clickable { onClick() } else m }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}
