package com.flowtune.tv.online

import android.util.Base64
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.AsyncFunctionBinding
import com.dokar.quickjs.binding.FunctionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * LX 自定义音源脚本引擎（quickjs-kt）。
 * 实现 lx-music 音源脚本 API：lx.request / lx.on / lx.send / lx.utils（crypto/buffer/zlib）。
 */
class LxSourceEngine private constructor(private val quickJs: QuickJs) {

    data class ScriptInfo(val name: String, val version: String, val author: String)

    companion object {
        const val VERSION = "2.0.0"

        fun parseScriptMeta(script: String): ScriptInfo {
            val comment = Regex("^/[\\s\\S]+?\\*/").find(script)?.value ?: ""
            fun get(key: String) = Regex("@$key\\s+(.+)").find(comment)?.groupValues?.get(1)?.trim()?.take(56) ?: ""
            return ScriptInfo(get("@name").ifBlank { "未命名音源" }, get("@version").ifBlank { "0.0.0" }, get("@author"))
        }

        /** 加载并初始化脚本（验证 lx.on 注册成功）。 */
        suspend fun load(script: String, info: ScriptInfo): LxSourceEngine {
            val qjs = QuickJs.create(Dispatchers.IO)
            val engine = LxSourceEngine(qjs)
            engine.registerBridges()
            qjs.evaluate<Any?>(engine.bootstrapJs(info), filename = "lx-api.js")
            qjs.evaluate<Any?>(script, filename = "source.js")
            val ready = qjs.evaluate<Boolean>("typeof globalThis.__lxHandler === 'function'")
            if (!ready) throw RuntimeException("音源脚本未注册 request 处理器")
            return engine
        }
    }

    private fun md5Hex(bytes: ByteArray) = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }

    private suspend fun registerBridges() {
        // 同步 HTTP（JS 视角阻塞式，返回 {statusCode, headers, body} JSON）
        quickJs.defineBinding("__nativeHttp", FunctionBinding { args ->
            android.util.Log.d("FlowTune/Online", "__nativeHttp: " + (args?.getOrNull(0) as? String ?: "null").take(120))
            val req = org.json.JSONObject(args?.getOrNull(0) as? String ?: "{}")
            val builder = okhttp3.Request.Builder().url(req.getString("url"))
            val headers = req.optJSONObject("headers") ?: org.json.JSONObject()
            for (k in headers.keys()) builder.header(k, headers.optString(k))
            val method = req.optString("method", "GET").uppercase()
            val bodyStr = req.optString("body", "").ifEmpty { null }
            val form = req.optJSONObject("form")
            when (method) {
                "POST", "PUT" -> {
                    val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
                    val payload = when {
                        form != null -> {
                            builder.header("Content-Type", "application/x-www-form-urlencoded")
                            val sb = StringBuilder()
                            for (k in form.keys()) {
                                if (sb.isNotEmpty()) sb.append('&')
                                sb.append(java.net.URLEncoder.encode(k, "UTF-8")).append('=')
                                    .append(java.net.URLEncoder.encode(form.optString(k), "UTF-8"))
                            }
                            sb.toString()
                        }
                        else -> bodyStr ?: ""
                    }
                    builder.method(method, payload.toRequestBody(mediaType))
                }
                else -> builder.get()
            }
            Platforms.http.newCall(builder.build()).execute().use { resp ->
                val text = resp.body!!.string()
                val headersJson = org.json.JSONObject()
                for ((k, v) in resp.headers) headersJson.put(k, v)
                org.json.JSONObject()
                    .put("statusCode", resp.code)
                    .put("headers", headersJson)
                    .put("body", text)
                    .toString()
            }
        })

        quickJs.defineBinding("__md5", FunctionBinding { args ->
            val s = args?.getOrNull(0) as? String ?: ""
            val bytes = when (val enc = args.getOrNull(1) as? String) {
                "base64" -> Base64.decode(s, Base64.NO_WRAP)
                "hex" -> s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                else -> s.toByteArray()
            }
            md5Hex(bytes)
        })

        quickJs.defineBinding("__aes", FunctionBinding { args ->
            val data = Base64.decode(args?.getOrNull(0) as? String ?: "", Base64.NO_WRAP)
            val mode = args.getOrNull(1) as? String ?: "aes-128-ecb"
            val key = Base64.decode(args.getOrNull(2) as? String ?: "", Base64.NO_WRAP)
            val iv = args.getOrNull(3) as? String
            val cipher = when {
                mode.contains("ecb", true) -> Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
                    init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
                }
                mode.contains("cbc", true) -> Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                    init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(Base64.decode(iv ?: "", Base64.NO_WRAP)))
                }
                else -> throw IllegalArgumentException("unsupported aes mode: $mode")
            }
            Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP)
        })

        quickJs.defineBinding("__aesDec", FunctionBinding { args ->
            val data = Base64.decode(args?.getOrNull(0) as? String ?: "", Base64.NO_WRAP)
            val mode = args.getOrNull(1) as? String ?: "aes-128-ecb"
            val key = Base64.decode(args.getOrNull(2) as? String ?: "", Base64.NO_WRAP)
            val iv = args.getOrNull(3) as? String
            val cipher = when {
                mode.contains("ecb", true) -> Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
                }
                mode.contains("cbc", true) -> Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(Base64.decode(iv ?: "", Base64.NO_WRAP)))
                }
                else -> throw IllegalArgumentException("unsupported aes mode: $mode")
            }
            Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP)
        })

        quickJs.defineBinding("__randomBytes", FunctionBinding { args ->
            val n = (args?.getOrNull(0) as? Number)?.toInt() ?: 16
            val buf = ByteArray(n)
            SecureRandom().nextBytes(buf)
            Base64.encodeToString(buf, Base64.NO_WRAP)
        })

        quickJs.defineBinding("__inflate", FunctionBinding { args ->
            val data = Base64.decode(args?.getOrNull(0) as? String ?: "", Base64.NO_WRAP)
            val inflater = Inflater()
            inflater.setInput(data)
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0 && inflater.needsInput()) break
                out.write(buf, 0, n)
            }
            inflater.end()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        })

        quickJs.defineBinding("__deflate", FunctionBinding { args ->
            val data = Base64.decode(args?.getOrNull(0) as? String ?: "", Base64.NO_WRAP)
            val deflater = Deflater()
            deflater.setInput(data)
            deflater.finish()
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
            deflater.end()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        })
    }

    /**
     * 调用脚本注册的 request 处理器（musicUrl/pic 等）。
     * 返回处理器结果（通常为 URL 字符串）。
     */
    suspend fun callHandler(action: String, source: String, infoJson: String, timeoutMs: Long = 25_000): String {
        quickJs.evaluate<Any?>("globalThis.__lxResult = null;")
        quickJs.evaluate<Any?>(
            "__lxCallFire(${jStr(action)}, ${jStr(source)}, ${jStr(infoJson)});",
            filename = "lx-fire.js"
        )
        // 轮询触发 Promise 任务泵：quickjs-kt 每次 evaluate 都会执行 pending jobs
        withTimeout(timeoutMs) {
            while (true) {
                delay(25)
                val r = quickJs.evaluate<String?>("globalThis.__lxResult")
                if (r != null) break
            }
        }
        val raw = quickJs.evaluate<String?>("globalThis.__lxResult")
            ?: throw RuntimeException("音源响应超时")
        android.util.Log.d("FlowTune/Online", "lx callHandler action=$action -> $raw")
        val obj = org.json.JSONObject(raw)
        if (!obj.optBoolean("ok")) throw RuntimeException(obj.optString("msg", "音源返回失败"))
        return when (val data = obj.opt("data")) {
            is String -> data
            is org.json.JSONObject -> data.optString("url", "").ifEmpty { throw RuntimeException("音源未返回 URL") }
            else -> throw RuntimeException("音源返回格式异常")
        }
    }

    private fun jStr(s: String) = org.json.JSONObject.quote(s)

    /** lx API 引导脚本：实现 lx 对象与 LX 音源脚本约定的桥接。 */
    private fun bootstrapJs(info: ScriptInfo): String = """
'use strict';
(function() {
  var b64ToU8 = function(b64) {
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    var clean = b64.replace(/[^A-Za-z0-9+/=]/g, '');
    var bytes = [];
    for (var i = 0; i < clean.length; i += 4) {
      var e1 = chars.indexOf(clean.charAt(i)), e2 = chars.indexOf(clean.charAt(i+1));
      var e3 = chars.indexOf(clean.charAt(i+2)), e4 = chars.indexOf(clean.charAt(i+3));
      var n = (e1 << 18) | (e2 << 12) | ((e3 & 63) << 6) | (e4 & 63);
      bytes.push((n >> 16) & 0xFF);
      if (clean.charAt(i+2) !== '=') bytes.push((n >> 8) & 0xFF);
      if (clean.charAt(i+3) !== '=') bytes.push(n & 0xFF);
    }
    return new Uint8Array(bytes);
  };
  var u8ToB64 = function(u8) {
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    var out = '';
    for (var i = 0; i < u8.length; i += 3) {
      var b1 = u8[i], b2 = i+1 < u8.length ? u8[i+1] : 0, b3 = i+2 < u8.length ? u8[i+2] : 0;
      out += chars.charAt(b1 >> 2) + chars.charAt(((b1 & 3) << 4) | (b2 >> 4));
      out += i+1 < u8.length ? chars.charAt(((b2 & 15) << 2) | (b3 >> 6)) : '=';
      out += i+2 < u8.length ? chars.charAt(b3 & 63) : '=';
    }
    return out;
  };
  var u8ToHex = function(u8) { var s=''; for (var i=0;i<u8.length;i++) s += ('0'+u8[i].toString(16)).slice(-2); return s; };
  var hexToU8 = function(hex) { var u8 = new Uint8Array(hex.length >> 1); for (var i=0;i<u8.length;i++) u8[i] = parseInt(hex.substr(i*2,2),16); return u8; };
  var utf8ToU8 = function(s) {
    var out = [], i, c;
    for (i = 0; i < s.length; i++) {
      c = s.codePointAt(i);
      if (c < 0x80) out.push(c);
      else if (c < 0x800) { out.push(0xC0|(c>>6), 0x80|(c&63)); }
      else if (c < 0x10000) { out.push(0xE0|(c>>12), 0x80|((c>>6)&63), 0x80|(c&63)); }
      else { out.push(0xF0|(c>>18), 0x80|((c>>12)&63), 0x80|((c>>6)&63), 0x80|(c&63)); i++; }
    }
    return new Uint8Array(out);
  };
  var u8ToUtf8 = function(u8) {
    try { return decodeURIComponent(escape(String.fromCharCode.apply(null, u8))); } catch (e) { return ''; }
  };

  globalThis.lx = {
    EVENT_NAMES: { request: 'request', inited: 'inited', updateAlert: 'updateAlert' },
    version: '$VERSION',
    env: 'android',
    currentScriptInfo: {
      name: ${jStr(info.name)},
      version: ${jStr(info.version)},
      author: ${jStr(info.author)},
      rawScript: ''
    },
    request: function(url, opts, callback) {
      var o = opts || {};
      var payload = {
        url: url,
        method: o.method || 'GET',
        headers: o.headers || {},
        body: o.body || '',
        form: o.form || null,
        timeout: o.timeout || 15000
      };
      var p = Promise.resolve(__nativeHttp(JSON.stringify(payload))).then(function(rj) {
        var resp = (typeof rj === 'string') ? JSON.parse(rj) : rj;
        var body = resp.body;
        var parsed;
        try { parsed = (typeof body === 'string') ? JSON.parse(body) : body; } catch (e) { parsed = body; }
        resp.body = parsed;
        resp.raw = { toString: function() { return body; } };
        resp.bytes = body.length;
        return { resp: resp, parsed: parsed };
      });
      if (typeof callback === 'function') {
        p.then(function(r) { callback(null, r.resp, r.parsed); },
               function(e) { callback(e, null, null); });
      }
      return p.then(function(r) { return r.resp; });
    },
    on: function(eventName, handler) {
      if (eventName === 'request') { globalThis.__lxHandler = handler; return Promise.resolve(); }
      return Promise.reject(new Error('The event is not supported: ' + eventName));
    },
    send: function(eventName, data) {
      return Promise.resolve();
    },
    utils: {
      crypto: {
        md5: function(str, encoding) { return __md5(String(str), encoding || 'utf8'); },
        aesEncrypt: function(buf, mode, key, iv) {
          return b64ToU8(__aes(u8ToB64(buf), mode, u8ToB64(key), iv ? u8ToB64(iv) : null));
        },
        aesDecrypt: function(buf, mode, key, iv) {
          return b64ToU8(__aesDec(u8ToB64(buf), mode, u8ToB64(key), iv ? u8ToB64(iv) : null));
        },
        rsaEncrypt: function() { throw new Error('rsaEncrypt not supported'); },
        randomBytes: function(n) { return b64ToU8(__randomBytes(n)); },
      },
      buffer: {
        from: function(val, encoding) {
          if (typeof val === 'string') {
            if (encoding === 'base64') return b64ToU8(val);
            if (encoding === 'hex') return hexToU8(val);
            return utf8ToU8(val);
          }
          return new Uint8Array(val);
        },
        bufToString: function(buf, encoding) {
          if (encoding === 'base64') return u8ToB64(buf);
          if (encoding === 'hex') return u8ToHex(buf);
          return u8ToUtf8(buf);
        },
      },
      zlib: {
        inflate: function(buf) { return Promise.resolve(b64ToU8(__inflate(u8ToB64(buf)))); },
        deflate: function(data) { return Promise.resolve(b64ToU8(__deflate(u8ToB64(typeof data === 'string' ? utf8ToU8(data) : data)))); },
      },
    },
  };

  globalThis.__lxResult = null;
  globalThis.__lxCallFire = function(action, source, infoJson) {
    var handler = globalThis.__lxHandler;
    if (!handler) { globalThis.__lxResult = JSON.stringify({ok:false, msg:'音源脚本未初始化'}); return; }
    try {
      var info = (typeof infoJson === 'string') ? JSON.parse(infoJson) : infoJson;
      var p = Promise.resolve(handler({ source: source, action: action, info: info }));
      p.then(function(v) {
        if (v && typeof v === 'object' && typeof v.url === 'string') v = v.url;
        globalThis.__lxResult = JSON.stringify({ ok: true, data: v });
      }).catch(function(e) {
        globalThis.__lxResult = JSON.stringify({ ok: false, msg: String(e && e.message || e) });
      });
    } catch (e) {
      globalThis.__lxResult = JSON.stringify({ ok: false, msg: String(e && e.message || e), stack: String(e && e.stack || '').slice(0, 300) });
    }
  };
})();
"""
}
