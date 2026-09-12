package com.flowtune.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.flowtune.tv.data.LyricLine
import com.flowtune.tv.model.EffectLevel
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * 逐字歌词 Canvas 渲染（AMLL 风格）：
 * - OFF 档：15fps 逐行高亮，无弹簧/模糊，老盒子零压力
 * - LOW：20fps + 封面倒影
 * - MEDIUM：30fps + 逐字渐变填充 + 弹簧滚动
 * - HIGH：45fps + 未唱行模糊（依内核能力降级为透明度）
 *
 * 帧泵在 LaunchedEffect 里按档位 fps 更新 drawPos/smoothScroll；draw 纯读状态。
 * 绝不能在 draw 里写 remember 状态——会触发重绘循环且跳帧时画空白帧（闪烁根因）。
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
    val spring = effectLevel.wordSpring
    val currentPos by androidx.compose.runtime.rememberUpdatedState(positionMs)

    var drawPos by remember { mutableLongStateOf(0L) }
    var smoothScroll by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }
    var snapScroll by remember { mutableStateOf(true) }
    LaunchedEffect(lyrics) { snapScroll = true }

    LaunchedEffect(frameInterval, spring) {
        while (true) {
            drawPos = currentPos
            if (canvasH > 0f && lyrics.isNotEmpty()) {
                val lineHeight = canvasH * LINE_H_RATIO
                val idx = currentIndex(lyrics, drawPos)
                if (idx >= 0) {
                    val target = idx * lineHeight - canvasH * HEAD_RATIO
                    smoothScroll = when {
                        snapScroll -> { snapScroll = false; target }
                        spring -> smoothScroll + (target - smoothScroll) * 0.12f
                        else -> target
                    }
                }
            }
            delay(frameInterval)
        }
    }

    Canvas(modifier.onSizeChanged { canvasH = it.height.toFloat() }) {
        if (lyrics.isEmpty()) return@Canvas
        val lineHeight = size.height * LINE_H_RATIO
        val currentIdx = currentIndex(lyrics, drawPos)
        if (currentIdx < 0) return@Canvas

        val visibleRange = (currentIdx - 3)..(currentIdx + 5)
        for (i in visibleRange) {
            val line = lyrics.getOrNull(i) ?: continue
            if (line.text.isBlank()) continue
            val y = i * lineHeight - smoothScroll
            if (y < -lineHeight || y > size.height + lineHeight) continue

            val isCurrent = i == currentIdx
            val style = TextStyle(
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.34f),
                fontSize = (if (isCurrent) 30 else 26).sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            )
            val measured = measurer.measure(line.text, style)
            val x = (size.width - measured.size.width) / 2f
            val lineY = y + (lineHeight - measured.size.height) / 2f

            if (isCurrent && line.words.isNotEmpty()) {
                // 逐字渐变填充：已唱部分亮、未唱暗
                drawWordByWord(measurer, line, style, x, lineY, size.width, drawPos)
            } else {
                drawText(measured, topLeft = Offset(x, lineY))
            }
        }
    }
}

private const val LINE_H_RATIO = 0.115f
private const val HEAD_RATIO = 0.42f

private fun currentIndex(lyrics: List<LyricLine>, pos: Long): Int =
    lyrics.indexOfFirst { pos in it.startMs until it.endMs }
        .let { if (it == -1) lyrics.indexOfLast { l -> l.startMs <= pos } else it }

private fun DrawScope.drawWordByWord(
    measurer: TextMeasurer,
    line: LyricLine,
    style: TextStyle,
    x: Float,
    y: Float,
    maxWidth: Float,
    pos: Long,
) {
    val sungColor = style.color
    val unsungColor = Color.White.copy(alpha = 0.45f)

    var cursor = x
    for (word in line.words) {
        val wMeas = measurer.measure(word.text, style)
        val wProg = ((pos - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1)).coerceIn(0f, 1f)
        // 字宽按进度裁切：画两次（底色 + 高亮裁切）
        drawText(wMeas, color = unsungColor, topLeft = Offset(cursor, y))
        if (wProg > 0f) {
            withTransform({ clipRect(cursor, y, cursor + wMeas.size.width * wProg, y + wMeas.size.height) }) {
                drawText(wMeas, color = sungColor, topLeft = Offset(cursor, y))
            }
        }
        cursor += wMeas.size.width
        if (cursor > maxWidth) break
    }
}
