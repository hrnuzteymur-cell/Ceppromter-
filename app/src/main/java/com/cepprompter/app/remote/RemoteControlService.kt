package com.cepprompter.app.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cepprompter.app.control.ControlCenter
import com.cepprompter.app.control.PrompterCommand
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.URLDecoder
import java.util.concurrent.Executors

class RemoteControlService : Service() {
    private var socket: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool()
    private var preview = ""

    override fun onCreate() {
        super.onCreate(); createChannel()
        startForeground(42, NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Cep Prompter Wi‑Fi kumandası").setContentText("8765 numaralı bağlantı noktası açık").setOngoing(true).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        preview = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        if (socket == null) pool.execute { serve() }
        return START_STICKY
    }
    private fun serve() {
        runCatching {
            ServerSocket(PORT).also { socket = it }.use { server ->
                while (!server.isClosed) {
                    val client = server.accept()
                    pool.execute {
                        client.use {
                            val line = BufferedReader(InputStreamReader(it.getInputStream())).readLine().orEmpty()
                            val path = line.split(' ').getOrNull(1).orEmpty()
                            handle(path)
                            val html = page()
                            val bytes = html.toByteArray()
                            it.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            it.getOutputStream().write(bytes)
                        }
                    }
                }
            }
        }
    }
    private fun handle(path: String) {
        when {
            path.startsWith("/cmd/toggle") -> ControlCenter.send(PrompterCommand.Toggle)
            path.startsWith("/cmd/play") -> ControlCenter.send(PrompterCommand.Play)
            path.startsWith("/cmd/pause") -> ControlCenter.send(PrompterCommand.Pause)
            path.startsWith("/cmd/faster") -> ControlCenter.send(PrompterCommand.Faster)
            path.startsWith("/cmd/slower") -> ControlCenter.send(PrompterCommand.Slower)
            path.startsWith("/cmd/seek") -> path.substringAfter("p=", "0").toFloatOrNull()?.let { ControlCenter.send(PrompterCommand.Seek((it / 100f).coerceIn(0f, 1f))) }
        }
    }
    private fun page(): String {
        val escaped = preview.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        return """<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'><style>body{font-family:sans-serif;background:#071018;color:white;margin:0;padding:20px}h1{color:#65e3bc}button{font-size:20px;padding:16px;margin:5px;background:#14362f;color:white;border:1px solid #65e3bc;border-radius:12px}.preview{font-size:24px;line-height:1.5;max-height:48vh;overflow:auto;background:#000;padding:18px;border-radius:16px}input{width:100%}</style><h1>Cep Prompter Kumanda</h1><button onclick="go('slower')">− Hız</button><button onclick="go('toggle')">▶ / Ⅱ</button><button onclick="go('faster')">+ Hız</button><input type=range min=0 max=100 value=0 oninput="fetch('/cmd/seek?p='+this.value)"><p>Aynı Wi‑Fi ağında uzaktan kontrol ve metin önizleme</p><div class=preview>$escaped</div><script>function go(x){fetch('/cmd/'+x)}</script>"""
    }
    override fun onDestroy() { runCatching { socket?.close() }; socket = null; pool.shutdownNow(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL, "Wi-Fi kumandası", NotificationManager.IMPORTANCE_LOW)) }
    companion object { const val PORT = 8765; const val EXTRA_TEXT = "text"; const val ACTION_STOP = "stop"; private const val CHANNEL = "remote_prompter" }
}
