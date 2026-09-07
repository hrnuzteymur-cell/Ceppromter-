package com.cepprompter.app.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.cepprompter.app.control.ControlCenter
import com.cepprompter.app.control.PrompterCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class OverlayPrompterService : Service() {
    private lateinit var manager: WindowManager
    private var root: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var playing = false
    private var speed = 4
    private var commandJob: Job? = null

    private val ticker = object : Runnable {
        override fun run() {
            if (playing) root?.findViewById<ScrollView>(1002)?.scrollBy(0, speed)
            handler.postDelayed(this, 32)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(41, NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Cep Prompter kayan pencere").setContentText("Başka uygulamaların üzerinde çalışıyor").setOngoing(true).build())
        commandJob = CoroutineScope(Dispatchers.Main).launch {
            ControlCenter.commands.collect { command ->
                when (command) {
                    PrompterCommand.Toggle -> playing = !playing
                    PrompterCommand.Play -> playing = true
                    PrompterCommand.Pause -> playing = false
                    PrompterCommand.Faster -> speed = (speed + 1).coerceAtMost(18)
                    PrompterCommand.Slower -> speed = (speed - 1).coerceAtLeast(1)
                    is PrompterCommand.Seek -> root?.findViewById<ScrollView>(1002)?.let { it.scrollTo(0, (it.getChildAt(0).height * command.progress).toInt()) }
                }
            }
        }
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        show(intent?.getStringExtra(EXTRA_TEXT).orEmpty())
        return START_STICKY
    }

    private fun show(text: String) {
        root?.let { manager.removeView(it) }
        manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * .9f).toInt(), (260 * density).toInt(),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 160 }

        val title = TextView(this).apply { id = 1001; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(16, 29, 44)); setPadding(18, 10, 18, 10); this.text = "☰  CEP PROMPTER — sürükle" }
        val body = TextView(this).apply { setTextColor(Color.WHITE); textSize = 30f; setPadding(22, 120, 22, 160); this.text = text.ifBlank { "Metin seçilmedi" } }
        val scroll = ScrollView(this).apply { id = 1002; addView(body); setBackgroundColor(Color.argb(210, 0, 0, 0)) }
        val play = Button(this).apply { text = "▶ / Ⅱ"; setOnClickListener { playing = !playing } }
        val minus = Button(this).apply { text = "−"; setOnClickListener { speed = (speed - 1).coerceAtLeast(1) } }
        val plus = Button(this).apply { text = "+"; setOnClickListener { speed = (speed + 1).coerceAtMost(18) } }
        val close = Button(this).apply { text = "Kapat"; setOnClickListener { stopSelf() } }
        val resize = SeekBar(this).apply { max = 100; progress = 55; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) { params.height = ((160 + value * 4) * density).toInt(); root?.let { manager.updateViewLayout(it, params) } } }
            override fun onStartTrackingTouch(s: SeekBar?) = Unit; override fun onStopTrackingTouch(s: SeekBar?) = Unit
        }) }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.rgb(16,29,44)); addView(minus); addView(play); addView(plus); addView(resize, LinearLayout.LayoutParams(0, -2, 1f)); addView(close) }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; alpha = .92f; addView(title); addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); addView(controls) }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        title.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y; true }
                MotionEvent.ACTION_MOVE -> { params.x = startX + (e.rawX - downX).toInt(); params.y = startY + (e.rawY - downY).toInt(); manager.updateViewLayout(container, params); true }
                else -> true
            }
        }
        root = container
        manager.addView(container, params)
    }

    override fun onDestroy() { handler.removeCallbacks(ticker); commandJob?.cancel(); root?.let { runCatching { manager.removeView(it) } }; root = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL, "Kayan prompter", NotificationManager.IMPORTANCE_LOW)) }
    companion object { const val EXTRA_TEXT = "text"; const val ACTION_STOP = "stop"; private const val CHANNEL = "overlay_prompter" }
}
