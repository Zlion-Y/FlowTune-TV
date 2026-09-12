package com.flowtune.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.flowtune.tv.data.LyricLine
import com.flowtune.tv.model.EffectLevel
import com.flowtune.tv.model.Song
import kotlin.math.min

/**
 * 逐字歌词 Canvas 渲染（AMLL 风格）：
 * - OFF 档：15fps 逐行高亮，无弹簧/模糊，老盒子零压力
 * - LOW：20fps + 封面倒影
 * - MEDIUM：30fps + 逐字渐变填充 + 弹簧滚动
 * - HIGH：45fps + 未唱行模糊（依内核能力降级为透明度）
 */
@Composable
fun LyricsCanvas(
    lyrics: List<LyricLine>,
    positionMs: Long,
    effectLevel: EffectLevel,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val frameInterval = (1000 / effectLevel.lyricFps).toLong()
    var frameTime by remember { mutableStateOf(0L) }

    Canvas(modifier) {
        if (lyrics.isEmpty()) return@Canvas
        val now = System.currentTimeMillis()
        if (now - frameTime < frameInterval) return@Canvas
        frameTime = now

        val lineHeight = size.height * 0.115f
        val currentIdx = lyrics.indexOfFirst { positionMs in it.startMs until it.endMs }
            .let { if (it == -1) lyrics.indexOfLast { l -> l.startMs <= positionMs } else it }
        if (currentIdx < 0) return@Canvas

        val spring = effectLevel.wordSpring
        // 弹簧平滑：目标偏移按当前行插值（MEDIUM+ 档）
        val targetOffset = currentIdx * lineHeight - size.height * 0.42f
        smoothScroll = if (spring) smoothScroll + (targetOffset - smoothScroll) * 0.12f else targetOffset

        val visibleRange = (currentIdx - 3)..(currentIdx + 5)
        for (i in visibleRange) {
            val line = lyrics.getOrNull(i) ?: continue
            if (line.text.isBlank()) continue
            val y = i * lineHeight - smoothScroll
            if (y < -lineHeight || y > size.height + lineHeight) continue

            val isCurrent = i == currentIdx
            val style = TextStyle(
                color = when {
                    isCurrent -> Color.White
                    else -> Color.White.copy(alpha = 0.34f)
                },
                fontSize = (if (isCurrent) 30 else 26).sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            )
            val measured = measurer.measure(line.text, style)
            val x = (size.width - measured.size.width) / 2f
            val lineY = y + (lineHeight - measured.size.height) / 2f

            if (isCurrent && line.words.isNotEmpty()) {
                // 逐字渐变填充：已唱部分亮、未唱暗
                drawWordByWord(measurer, line, style, x, lineY, size.width)
            } else {
                drawText(measured, topLeft = Offset(x, lineY))
            }
        }
    }
}

private var smoothScroll = 0f

private fun DrawScope.drawWordByWord(
    measurer: TextMeasurer,
    line: LyricLine,
    style: TextStyle,
    x: Float,
    y: Float,
    maxWidth: Float,
) {
    val progress = (positionProvider() - line.startMs).toFloat() / (line.endMs - line.startMs).coerceAtLeast(1)
    val sungColor = style.color
    val unsungColor = Color.White.copy(alpha = 0.45f)

    var cursor = x
    for (word in line.words) {
        val wMeas = measurer.measure(word.text, style)
        val wProg = ((positionProvider() - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1)).coerceIn(0f, 1f)
        // 字宽按进度裁切：画两次（底色 + 高亮裁切）
        drawText(wMeas, color = unsungColor, topLeft = Offset(cursor, y))
        if (wProg > 0f) {
            clipRect(cursor, y, cursor + wMeas.size.width * wProg, y + wMeas.size.height) {
                drawText(wMeas, color = sungColor, topLeft = Offset(cursor, y))
            }
        }
        cursor += wMeas.size.width
        if (cursor > maxWidth) break
    }
}

// positionMs 由调用方每帧写入（避免跨层传参）
private var positionProvider: () -> Long = { 0L }

@Composable
fun SyncLyricsPosition(positionMs: Long) {
    DisposableEffect(positionMs) {
        positionProvider = { positionMs }
        onDispose { }
    }
}

private fun DrawScope.clipRect(l: Float, t: Float, r: Float, b: Float, block: DrawScope.() -> Unit) {
    withTransform({ clipRect(l, t, r, b) }, block)
}
