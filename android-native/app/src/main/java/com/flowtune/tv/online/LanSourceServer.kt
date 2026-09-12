package com.flowtune.tv.online

import android.graphics.Bitmap
import android.graphics.Color
import fi.iki.elonen.NanoHTTPD
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.net.NetworkInterface

/**
 * 局域网音源导入服务：手机/电脑浏览器打开 http://<TV_IP>:9420 上传 .js 音源脚本，
 * 收到文件后通过 uploads 流交给设置页导入。附带二维码生成。
 */
object LanSourceServer {

    private const val PORT = 9420
    private var server: NanoHTTPD? = null

    private val _uploads = MutableSharedFlow<File>(extraBufferCapacity = 8)
    val uploads: SharedFlow<File> get() = _uploads

    val isRunning: Boolean get() = server != null

    fun start(cacheDir: File, onLog: (String) -> Unit): String? {
        if (server != null) return localAddress()
        val ip = localAddress() ?: run { onLog("未获取到局域网 IP（请确认已连网）"); return null }
        try {
            server = object : NanoHTTPD(PORT) {
                override fun serve(session: IHTTPSession): Response {
                    return if (session.method == Method.POST && session.uri == "/upload") {
                        val files = HashMap<String, String>()
                        session.parseBody(files)
                        val tmp = files["jsfile"] ?: return newFixedLengthResponse(
                            Response.Status.BAD_REQUEST, "text/html; charset=utf-8", "未选择文件"
                        )
                        val saved = File(cacheDir, "lx-lan-${System.currentTimeMillis()}.js")
                        File(tmp).copyTo(saved, overwrite = true)
                        _uploads.tryEmit(saved)
                        newFixedLengthResponse(
                            Response.Status.OK, "text/html; charset=utf-8",
                            "<meta charset='utf-8'><body style='background:#14161A;color:#fff;font-size:20px;text-align:center;padding-top:40px'>✅ 上传成功，电视端正在导入<br><a style='color:#5B9BFF' href='/'>继续上传</a></body>"
                        )
                    } else {
                        newFixedLengthResponse(
                            Response.Status.OK, "text/html; charset=utf-8", UPLOAD_PAGE
                        )
                    }
                }
            }.apply { start(5000, false) }
            onLog("局域网导入服务已启动：http://$ip:$PORT")
            return "http://$ip:$PORT"
        } catch (e: Exception) {
            server = null
            onLog("启动失败：${e.message}")
            return null
        }
    }

    fun stop() {
        server?.stop()
        server = null
    }

    fun localAddress(): String? = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }

    fun qrBitmap(content: String, size: Int = 400): Bitmap {
        val matrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            pixels[y * size + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.RGB_565)
    }

    private const val UPLOAD_PAGE = """
<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
<body style='background:#14161A;color:#fff;font-family:sans-serif;max-width:560px;margin:0 auto;padding:28px'>
<h2 style='color:#5B9BFF'>FlowTune 音源上传</h2>
<p style='color:#9AA'>选择 .js 音源脚本上传，电视端将自动导入。</p>
<form method='POST' action='/upload' enctype='multipart/form-data'>
<input type='file' name='jsfile' accept='.js' required
 style='color:#fff;margin:16px 0'>
<br><button style='background:#5B9BFF;color:#fff;border:0;border-radius:10px;padding:12px 28px;font-size:16px'>上传并导入</button>
</form>
</body>
"""
}
