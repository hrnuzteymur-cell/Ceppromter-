package com.cepprompter.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cepprompter.app.model.PrompterSettings
import com.cepprompter.app.model.Script
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.io.File
import java.util.Date
import java.util.Locale

@Composable
fun CameraRecorderScreen(
    script: Script,
    settings: PrompterSettings,
    onSettingsChange: (PrompterSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var permissionTick by remember { mutableIntStateOf(0) }
    val requestedPermissions = remember { buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT <= 28) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }.toTypedArray() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionTick++ }
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        if (!hasCamera || !hasAudio) permissionLauncher.launch(requestedPermissions)
    }

    if (!hasCamera) {
        PermissionMessage(onRequest = { permissionLauncher.launch(requestedPermissions) }, onBack = onBack)
        return
    }

    var frontCamera by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var playing by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(frontCamera, lifecycleOwner, permissionTick, settings.videoQuality) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val quality = when (settings.videoQuality) { "HD" -> Quality.HD; "UHD" -> Quality.UHD; else -> Quality.FHD }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
                ).build()
            val capture = VideoCapture.withOutput(recorder)
            runCatching {
                provider.unbindAll()
                val preferred = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val fallback = if (frontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                val selector = if (provider.hasCamera(preferred)) preferred else fallback
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture
                )
                videoCapture = capture
            }.onFailure { Toast.makeText(context, "Kamera başlatılamadı: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
            if (recording != null) recording?.stop()
        }
    }

    fun startOrStopRecording() {
        recording?.let {
            it.stop()
            recording = null
            playing = false
            return
        }
        if (countdown > 0) return
        scope.launch {
            countdown = settings.countdown
            while (countdown > 0) { delay(1000); countdown-- }
            val capture = videoCapture ?: return@launch
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            fun begin(pending: PendingRecording, legacyFile: File? = null) {
                var ready = pending
                if (hasAudio) ready = ready.withAudioEnabled()
                recording = ready.start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> playing = true
                        is VideoRecordEvent.Finalize -> {
                            recording = null
                            playing = false
                            if (!event.hasError() && legacyFile != null) {
                                MediaScannerConnection.scanFile(context, arrayOf(legacyFile.absolutePath), arrayOf("video/mp4"), null)
                            }
                            val message = if (event.hasError()) "Kayıt tamamlanamadı (${event.error})" else "Video galeriye kaydedildi"
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT < 29) {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Cep Prompter").apply { mkdirs() }
                val file = File(directory, "CepPrompter_$stamp.mp4")
                begin(capture.output.prepareRecording(context, FileOutputOptions.Builder(file).build()), file)
                return@launch
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "CepPrompter_$stamp")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Cep Prompter")
            }
            val output = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(values).build()
            begin(capture.output.prepareRecording(context, output))
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(camera) {
            detectTransformGestures { _, _, zoom, _ ->
                val state = camera?.cameraInfo?.zoomState?.value ?: return@detectTransformGestures
                camera?.cameraControl?.setZoomRatio((state.zoomRatio * zoom).coerceIn(state.minZoomRatio, state.maxZoomRatio))
            }
        }
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        CameraGrid()
        PrompterText(
            text = script.body,
            settings = settings,
            playing = playing,
            modifier = Modifier.align(Alignment.Center).fillMaxHeight(.68f),
            onPlayingChange = { playing = it }
        )

        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIcon(Icons.Default.Close, "Kapat", onBack)
            Surface(color = Color.Black.copy(alpha = .58f), shape = RoundedCornerShape(50)) {
                Text(if (recording == null) "HAZIR" else "● KAYIT", color = if (recording == null) Color.White else Color(0xFFFF6B6B), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            CircleIcon(Icons.Default.Cameraswitch, "Kamera değiştir") {
                if (recording == null) frontCamera = !frontCamera else Toast.makeText(context, "Kamerayı değiştirmek için kaydı durdurun", Toast.LENGTH_SHORT).show()
            }
        }

        if (!frontCamera) {
            Box(Modifier.align(Alignment.CenterEnd).padding(10.dp)) {
                CircleIcon(if (torch) Icons.Default.FlashOn else Icons.Default.FlashOff, "Flaş") {
                    torch = !torch
                    camera?.cameraControl?.enableTorch(torch)
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(Icons.Default.Remove, "Yazıyı küçült") { onSettingsChange(settings.copy(fontSize = (settings.fontSize - 2).coerceAtLeast(18f))) }
                FilledIconButton(
                    onClick = { startOrStopRecording() },
                    modifier = Modifier.size(76.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (recording == null) Color(0xFFE84949) else Color.White)
                ) {
                    Icon(if (recording == null) Icons.Default.FiberManualRecord else Icons.Default.Stop, "Kayıt", tint = if (recording == null) Color.White else Color(0xFFE84949), modifier = Modifier.size(42.dp))
                }
                CircleIcon(Icons.Default.Add, "Yazıyı büyüt") { onSettingsChange(settings.copy(fontSize = (settings.fontSize + 2).coerceAtMost(72f))) }
            }
            PrompterControls(playing, settings, { playing = it }, onSettingsChange)
        }

        if (countdown > 0) {
            Surface(Modifier.align(Alignment.Center), color = Color.Black.copy(alpha = .75f), shape = CircleShape) {
                Text(countdown.toString(), style = MaterialTheme.typography.displayLarge, color = Color.White, modifier = Modifier.padding(36.dp))
            }
        }
    }
}

@Composable
private fun PermissionMessage(onRequest: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("Kamera izni gerekli", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Video çekebilmek için kamera; ses kaydedebilmek için mikrofon izni verin.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("İzinleri aç") }
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Uygulama ayarlarına git") }
        TextButton(onClick = onBack) { Text("Geri dön") }
    }
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .58f))) {
        Icon(icon, description, tint = Color.White)
    }
}

@Composable
private fun CameraGrid() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = .22f)
        drawLine(c, start = androidx.compose.ui.geometry.Offset(size.width / 3, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 3, size.height), strokeWidth = 1f, cap = StrokeCap.Round)
        drawLine(c, start = androidx.compose.ui.geometry.Offset(size.width * 2 / 3, 0f), end = androidx.compose.ui.geometry.Offset(size.width * 2 / 3, size.height), strokeWidth = 1f, cap = StrokeCap.Round)
        drawLine(c, start = androidx.compose.ui.geometry.Offset(0f, size.height / 3), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 3), strokeWidth = 1f, cap = StrokeCap.Round)
        drawLine(c, start = androidx.compose.ui.geometry.Offset(0f, size.height * 2 / 3), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 2 / 3), strokeWidth = 1f, cap = StrokeCap.Round)
    }
}
