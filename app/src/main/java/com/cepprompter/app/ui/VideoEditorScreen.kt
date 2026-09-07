@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
package com.cepprompter.app.ui

import android.content.ContentValues
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.cepprompter.app.model.Script
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VideoEditorScreen(script: Script, onBack: () -> Unit) {
    val context = LocalContext.current
    var video by remember { mutableStateOf<Uri?>(null) }
    var logo by remember { mutableStateOf<Uri?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var startSeconds by remember { mutableFloatStateOf(0f) }
    var endSeconds by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableIntStateOf(0) }
    var aspect by remember { mutableStateOf("Orijinal") }
    var titleOverlay by remember { mutableStateOf("") }
    var burnCaptions by remember { mutableStateOf(false) }
    var captionText by remember(script.id) { mutableStateOf(script.body) }
    var exporting by remember { mutableStateOf(false) }
    var lastOutput by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Bir çekim seçin") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        video = uri
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
            startSeconds = 0f; endSeconds = durationMs / 1000f; status = "Video hazır: ${formatTime(durationMs)}"
        }.onFailure { status = "Video bilgisi okunamadı" }
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { logo = it }
    val srtExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(makeSrt(captionText, ((endSeconds - startSeconds) * 1000).toLong())) } }
            .onSuccess { Toast.makeText(context, "SRT kaydedildi", Toast.LENGTH_SHORT).show() }
    }

    fun exportVideo() {
        val input = video ?: return
        exporting = true; status = "Video cihaz üzerinde işleniyor…"
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val temp = File(context.cacheDir, "CepPrompter_Edit_$stamp.mp4").also { it.delete() }
        val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionMs((startSeconds * 1000).toLong())
            .setEndPositionMs((endSeconds * 1000).toLong().coerceAtLeast((startSeconds * 1000).toLong() + 100)).build()
        val item = MediaItem.Builder().setUri(input).setClippingConfiguration(clipping).build()
        val videoEffects = mutableListOf<Effect>()
        if (rotation != 0) videoEffects += ScaleAndRotateTransformation.Builder().setRotationDegrees(rotation.toFloat()).build()
        when (aspect) {
            "Dikey 9:16" -> videoEffects += Presentation.createForAspectRatio(9f / 16f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)
            "Kare 1:1" -> videoEffects += Presentation.createForAspectRatio(1f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)
            "Yatay 16:9" -> videoEffects += Presentation.createForAspectRatio(16f / 9f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)
        }
        val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()
        val visibleText = when { burnCaptions -> captionText.take(220); titleOverlay.isNotBlank() -> titleOverlay; else -> "" }
        if (visibleText.isNotBlank()) {
            val styled = SpannableString(visibleText).apply {
                setSpan(ForegroundColorSpan(android.graphics.Color.WHITE), 0, length, 0)
                setSpan(BackgroundColorSpan(android.graphics.Color.argb(180, 0, 0, 0)), 0, length, 0)
            }
            overlays += TextOverlay.createStaticTextOverlay(styled)
        }
        logo?.let { uri -> runCatching { context.contentResolver.openInputStream(uri)?.use(android.graphics.BitmapFactory::decodeStream) }.getOrNull()?.let { overlays += BitmapOverlay.createStaticBitmapOverlay(it) } }
        if (overlays.isNotEmpty()) videoEffects += OverlayEffect(overlays)
        val edited = EditedMediaItem.Builder(item).setEffects(Effects(emptyList(), videoEffects)).build()
        val transformer = Transformer.Builder(context).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, temp.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Cep Prompter")
                    }
                    val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    val target = context.contentResolver.insert(collection, values)
                    runCatching { requireNotNull(target); context.contentResolver.openOutputStream(target)?.use { out -> temp.inputStream().use { it.copyTo(out) } }; temp.delete() }
                        .onSuccess { lastOutput = target; status = "Düzenlenmiş video galeriye kaydedildi" }
                        .onFailure { status = "Video işlendi ancak galeriye kopyalanamadı" }
                    exporting = false
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    exporting = false; status = "Düzenleme tamamlanamadı: ${exportException.errorCodeName}"; temp.delete()
                }
            }).build()
        runCatching { transformer.start(edited, temp.absolutePath) }.onFailure { exporting = false; status = "Düzenleme başlatılamadı: ${it.localizedMessage}" }
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Video düzenleme stüdyosu") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { picker.launch(arrayOf("video/*")) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Movie, null); Spacer(Modifier.width(8.dp)); Text(if (video == null) "Galeriden video seç" else "Başka video seç") }
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (video != null) {
                Text("Kırpma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Başlangıç: ${"%.1f".format(startSeconds)} sn")
                Slider(startSeconds, { startSeconds = it.coerceAtMost(endSeconds - .1f) }, valueRange = 0f..(durationMs / 1000f).coerceAtLeast(.1f))
                Text("Bitiş: ${"%.1f".format(endSeconds)} sn")
                Slider(endSeconds, { endSeconds = it.coerceAtLeast(startSeconds + .1f) }, valueRange = 0f..(durationMs / 1000f).coerceAtLeast(.1f))
                Text("Döndürme")
                SingleChoiceSegmentedButtonRow { listOf(0,90,180,270).forEachIndexed { i, n -> SegmentedButton(rotation == n, { rotation = n }, SegmentedButtonDefaults.itemShape(i,4)) { Text("$n°") } } }
                Text("Çıktı oranı")
                listOf("Orijinal", "Dikey 9:16", "Kare 1:1", "Yatay 16:9").forEach { value -> FilterChip(aspect == value, { aspect = value }, { Text(value) }, Modifier.padding(end = 5.dp)) }
                OutlinedTextField(titleOverlay, { titleOverlay = it }, Modifier.fillMaxWidth(), label = { Text("Videoya logo yazısı / başlık") })
                OutlinedButton(onClick = { logoPicker.launch(arrayOf("image/*")) }) { Text(if (logo == null) "Logo görseli seç" else "Logo seçildi — değiştir") }
                Row { Text("Altyazıyı videoya bas", Modifier.weight(1f)); Switch(burnCaptions, { burnCaptions = it }) }
                OutlinedTextField(captionText, { captionText = it }, Modifier.fillMaxWidth().heightIn(min = 120.dp), label = { Text("Altyazı metni") })
                OutlinedButton(onClick = { srtExporter.launch("${script.title}.srt") }) { Text("Otomatik zamanla ve SRT dışa aktar") }
                Button(onClick = ::exportVideo, enabled = !exporting, modifier = Modifier.fillMaxWidth()) { if (exporting) CircularProgressIndicator(Modifier.size(20.dp)); Text(if (exporting) "  İşleniyor" else "Videoyu işle ve galeriye kaydet") }
                lastOutput?.let { uri -> OutlinedButton(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Videoyu paylaş")) }, Modifier.fillMaxWidth()) { Text("Düzenlenmiş videoyu paylaş") } }
            }
        }
    }
}

private fun makeSrt(text: String, durationMs: Long): String {
    val chunks = text.replace(Regex("\\s+"), " ").trim().split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.ifEmpty { listOf("Cep Prompter") }
    val slice = durationMs.coerceAtLeast(1000L) / chunks.size
    return chunks.mapIndexed { index, line -> "${index + 1}\n${srtTime(index * slice)} --> ${srtTime((index + 1) * slice)}\n$line\n" }.joinToString("\n")
}
private fun srtTime(ms: Long): String { val h = ms / 3_600_000; val m = ms / 60_000 % 60; val s = ms / 1000 % 60; val milli = ms % 1000; return "%02d:%02d:%02d,%03d".format(h,m,s,milli) }
private fun formatTime(ms: Long): String = "${ms / 60000}:${(ms / 1000 % 60).toString().padStart(2,'0')}"
