package com.cepprompter.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cepprompter.app.data.ScriptRepository
import com.cepprompter.app.model.AppPage
import com.cepprompter.app.model.PrompterSettings
import com.cepprompter.app.model.Script
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun CepPrompterApp(incomingText: String?, consumed: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ScriptRepository(context) }
    var scripts by remember { mutableStateOf(repository.load()) }
    var selected by remember { mutableStateOf(scripts.first()) }
    var page by remember { mutableStateOf(AppPage.LIBRARY) }
    var settings by remember { mutableStateOf(repository.loadSettings()) }

    fun persist(script: Script): Script {
        val saved = script.copy(updatedAt = System.currentTimeMillis())
        scripts = (scripts.filterNot { it.id == saved.id } + saved).sortedByDescending { it.updatedAt }
        repository.save(scripts)
        selected = saved
        return saved
    }

    fun updateSettings(value: PrompterSettings) {
        settings = value
        repository.saveSettings(value)
    }

    LaunchedEffect(incomingText) {
        incomingText?.takeIf { it.isNotBlank() }?.let {
            selected = Script(title = "Paylaşılan Metin", body = it)
            page = AppPage.EDITOR
            consumed()
        }
    }
    BackHandler(enabled = page != AppPage.LIBRARY) { page = AppPage.LIBRARY }

    when (page) {
        AppPage.LIBRARY -> LibraryScreen(
            scripts, { selected = it; page = AppPage.EDITOR },
            { selected = Script(title = "Yeni Metin", body = ""); page = AppPage.EDITOR },
            { selected = it; page = AppPage.PROMPTER }, { selected = it; page = AppPage.CAMERA },
            { persist(it.copy(favorite = !it.favorite)) },
            { persist(it.copy(id = java.util.UUID.randomUUID().toString(), title = "${it.title} (Kopya)")) },
            { target -> scripts = scripts.filterNot { it.id == target.id }.ifEmpty { listOf(Script(title = "Yeni Metin", body = "")) }; repository.save(scripts) },
            { persist(it) }, { page = AppPage.SETTINGS }
        )
        AppPage.EDITOR -> EditorScreen(selected, { page = AppPage.LIBRARY },
            { persist(it); page = AppPage.LIBRARY }, { persist(it); page = AppPage.PROMPTER }, { persist(it); page = AppPage.CAMERA })
        AppPage.PROMPTER -> PrompterScreen(selected, settings, repository.loadProgress(selected.id),
            { repository.saveProgress(selected.id, it) }, { updateSettings(it) }, { page = AppPage.LIBRARY }, { page = AppPage.CAMERA })
        AppPage.CAMERA -> CameraRecorderScreen(selected, settings, { updateSettings(it) }) { page = AppPage.LIBRARY }
        AppPage.SETTINGS -> SettingsScreen(settings, { updateSettings(it) }) { page = AppPage.LIBRARY }
    }
}

@Composable
private fun LibraryScreen(
    scripts: List<Script>, onOpen: (Script) -> Unit, onNew: () -> Unit,
    onPrompter: (Script) -> Unit, onCamera: (Script) -> Unit,
    onFavorite: (Script) -> Unit, onDuplicate: (Script) -> Unit, onDelete: (Script) -> Unit,
    onImport: (Script) -> Unit, onSettings: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "İçe Aktarılan Metin"
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                .replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            require(text.isNotBlank())
            onImport(Script(title = name, body = text))
        }.onFailure { Toast.makeText(context, "Dosya okunamadı. TXT, HTML veya RTF seçin.", Toast.LENGTH_LONG).show() }
    }
    val shown = scripts.filter { (!favoritesOnly || it.favorite) && (query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true)) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(
            title = { Column { Text("Cep Prompter", fontWeight = FontWeight.Bold); Text("Metnin hazır, kameran hazır", style = MaterialTheme.typography.labelSmall) } },
            actions = {
                IconButton(onClick = { importer.launch(arrayOf("text/*", "application/rtf")) }) { Icon(Icons.Default.FileOpen, "Dosya içe aktar") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Ayarlar") }
            }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Yeni metin") }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Metinlerde ara") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                FilterChip(favoritesOnly, { favoritesOnly = !favoritesOnly }, { Text("Yalnız favoriler") }, leadingIcon = { Icon(Icons.Default.Favorite, null) })
            }
            items(shown, key = { it.id }) { script ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onOpen(script) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(script.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { onFavorite(script) }) { Icon(if (script.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favori") }
                            IconButton(onClick = { onDuplicate(script) }) { Icon(Icons.Default.ContentCopy, "Çoğalt") }
                            IconButton(onClick = { onDelete(script) }) { Icon(Icons.Default.Delete, "Sil") }
                        }
                        Text(script.body.ifBlank { "Henüz metin eklenmedi" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onPrompter(script) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Oku") }
                            Button(onClick = { onCamera(script) }) { Icon(Icons.Default.Videocam, null); Spacer(Modifier.width(6.dp)); Text("Kamerayla çek") }
                        }
                    }
                }
            }
            if (shown.isEmpty()) item { Text("Aramana uygun metin bulunamadı.", Modifier.padding(24.dp)) }
        }
    }
}

@Composable
private fun EditorScreen(initial: Script, onBack: () -> Unit, onSave: (Script) -> Unit, onPrompt: (Script) -> Unit, onCamera: (Script) -> Unit) {
    val context = LocalContext.current
    var title by remember(initial.id) { mutableStateOf(initial.title) }
    var body by remember(initial.id) { mutableStateOf(initial.body) }
    val current = { initial.copy(title = title.ifBlank { "Adsız Metin" }, body = body, updatedAt = System.currentTimeMillis()) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(body) } }
            .onSuccess { Toast.makeText(context, "TXT dosyası kaydedildi", Toast.LENGTH_SHORT).show() }
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Metni düzenle") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") } }, actions = { IconButton(onClick = { exporter.launch("${title.ifBlank { "metin" }}.txt") }) { Icon(Icons.Default.SaveAlt, "TXT dışa aktar") } }) },
        bottomBar = { Surface(shadowElevation = 12.dp) { Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSave(current()) }, Modifier.weight(1f)) { Text("Kaydet") }
            FilledTonalButton(onClick = { onPrompt(current()) }, Modifier.weight(1f)) { Text("Oku") }
            Button(onClick = { onCamera(current()) }, Modifier.weight(1f)) { Text("Çek") }
        } } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Başlık") }, singleLine = true)
            OutlinedTextField(body, { body = it }, Modifier.fillMaxSize(), label = { Text("Okunacak metin") }, supportingText = { Text("${body.length} karakter • yaklaşık ${estimatedMinutes(body)} dakika") })
        }
    }
}

@Composable
fun PrompterText(text: String, settings: PrompterSettings, playing: Boolean, modifier: Modifier = Modifier,
    initialProgress: Float = 0f, onPlayingChange: (Boolean) -> Unit = {}, onProgressChange: (Float) -> Unit = {}) {
    val scroll = rememberScrollState()
    val latestPlaying by rememberUpdatedState(playing)
    val latestOnPlaying by rememberUpdatedState(onPlayingChange)
    LaunchedEffect(text) { delay(150); if (initialProgress > 0f) scroll.scrollTo((scroll.maxValue * initialProgress).roundToInt()) }
    LaunchedEffect(scroll) { snapshotFlow { if (scroll.maxValue == 0) 0f else scroll.value.toFloat() / scroll.maxValue }.distinctUntilChanged().collect { onProgressChange(it) } }
    LaunchedEffect(playing, settings.speed, settings.loop, settings.timedMinutes, text) {
        while (playing) {
            val timed = settings.timedMinutes?.let { scroll.maxValue / (it * 60f * 60f).coerceAtLeast(1f) }
            scroll.scrollBy(timed ?: (.55f + settings.speed * 7.5f))
            if (scroll.value >= scroll.maxValue) { if (settings.loop) scroll.scrollTo(0) else { onPlayingChange(false); break } }
            delay(16)
        }
    }
    Box(modifier.fillMaxWidth(settings.panelWidth).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = settings.panelOpacity))
        .graphicsLayer { scaleX = if (settings.mirrorHorizontal) -1f else 1f; scaleY = if (settings.mirrorVertical) -1f else 1f }
        .pointerInput(text) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val resumeAfterTouch = latestPlaying
                if (resumeAfterTouch) latestOnPlaying(false)
                var previousY = down.position.y
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position.y - previousY
                    if (delta != 0f) { scroll.scrollBy(-delta); change.consume() }
                    previousY = change.position.y
                } while (change.pressed)
                if (resumeAfterTouch) latestOnPlaying(true)
            }
        }) {
        Text(text.ifBlank { "Okunacak metin bulunmuyor." }, color = Color(settings.textColor.toULong()), fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineSpacing).sp, fontFamily = FontFamily.SansSerif,
            modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 160.dp))
        if (settings.highlightCenter) Box(Modifier.align(Alignment.Center).fillMaxWidth().height(3.dp).background(Color(0xFF26D7A0).copy(alpha = .8f)))
    }
}

@Composable
private fun PrompterScreen(script: Script, settings: PrompterSettings, initialProgress: Float, onProgress: (Float) -> Unit,
    onSettingsChange: (PrompterSettings) -> Unit, onBack: () -> Unit, onCamera: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(0xFF03070C)).systemBarsPadding()) {
        PrompterText(script.body, settings, playing, Modifier.align(Alignment.Center).fillMaxHeight(), initialProgress, { playing = it }, onProgress)
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Kapat", tint = Color.White) }
            Text(if (playing) "Dokun: beklet • Sürükle: gez" else "Hazır", color = Color.White, modifier = Modifier.padding(top = 12.dp))
            IconButton(onClick = onCamera) { Icon(Icons.Default.Videocam, "Kamera", tint = Color.White) }
        }
        PrompterControls(playing, settings, { playing = it }, onSettingsChange, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun PrompterControls(playing: Boolean, settings: PrompterSettings, onPlayingChange: (Boolean) -> Unit,
    onSettingsChange: (PrompterSettings) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = Color(0xE6101D2C), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            ControlSlider("Hız", "${(settings.speed * 100).roundToInt()}%", settings.speed, 0f..1f,
                { onSettingsChange(settings.copy(speed = it)) }, { onSettingsChange(settings.copy(speed = (settings.speed - .05f).coerceAtLeast(0f))) },
                { onSettingsChange(settings.copy(speed = (settings.speed + .05f).coerceAtMost(1f))) })
            ControlSlider("Saydamlık", "${(settings.panelOpacity * 100).roundToInt()}%", settings.panelOpacity, .1f..1f,
                { onSettingsChange(settings.copy(panelOpacity = it)) }, { onSettingsChange(settings.copy(panelOpacity = (settings.panelOpacity - .05f).coerceAtLeast(.1f))) },
                { onSettingsChange(settings.copy(panelOpacity = (settings.panelOpacity + .05f).coerceAtMost(1f))) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSettingsChange(settings.copy(fontSize = (settings.fontSize - 2).coerceAtLeast(18f))) }) { Icon(Icons.Default.TextDecrease, "Yazıyı küçült", tint = Color.White) }
                IconButton(onClick = { onSettingsChange(settings.copy(mirrorHorizontal = !settings.mirrorHorizontal)) }) { Icon(Icons.Default.Flip, "Yatay ayna", tint = if (settings.mirrorHorizontal) Color(0xFF26D7A0) else Color.White) }
                FilledIconButton(onClick = { onPlayingChange(!playing) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF26D7A0))) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Oynat/duraklat") }
                IconButton(onClick = { onSettingsChange(settings.copy(loop = !settings.loop)) }) { Icon(Icons.Default.Repeat, "Döngü", tint = if (settings.loop) Color(0xFF26D7A0) else Color.White) }
                IconButton(onClick = { onSettingsChange(settings.copy(fontSize = (settings.fontSize + 2).coerceAtMost(96f))) }) { Icon(Icons.Default.TextIncrease, "Yazıyı büyüt", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun ControlSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label $valueText", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(112.dp))
        IconButton(onClick = onMinus, Modifier.size(36.dp)) { Icon(Icons.Default.Remove, "$label azalt", tint = Color.White) }
        Slider(value, onValue, Modifier.weight(1f), valueRange = range)
        IconButton(onClick = onPlus, Modifier.size(36.dp)) { Icon(Icons.Default.Add, "$label artır", tint = Color.White) }
    }
}

@Composable
private fun SettingsScreen(settings: PrompterSettings, onSettingsChange: (PrompterSettings) -> Unit, onBack: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Ayarlar") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Okuma", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Yazı boyutu: ${settings.fontSize.roundToInt()} sp")
            Slider(settings.fontSize, { onSettingsChange(settings.copy(fontSize = it)) }, valueRange = 18f..96f)
            Text("Satır aralığı: ${"%.1f".format(settings.lineSpacing)}")
            Slider(settings.lineSpacing, { onSettingsChange(settings.copy(lineSpacing = it)) }, valueRange = 1f..2f)
            Text("Okuma alanı genişliği: ${(settings.panelWidth * 100).roundToInt()}%")
            Slider(settings.panelWidth, { onSettingsChange(settings.copy(panelWidth = it)) }, valueRange = .45f..1f)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Aktif satır çizgisi", Modifier.weight(1f)); Switch(settings.highlightCenter, { onSettingsChange(settings.copy(highlightCenter = it)) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Dikey ayna", Modifier.weight(1f)); Switch(settings.mirrorVertical, { onSettingsChange(settings.copy(mirrorVertical = it)) }) }
            Text("Geri sayım")
            SingleChoiceSegmentedButtonRow { listOf(0, 3, 5, 10).forEachIndexed { i, n -> SegmentedButton(selected = settings.countdown == n, onClick = { onSettingsChange(settings.copy(countdown = n)) }, shape = SegmentedButtonDefaults.itemShape(i, 4)) { Text(if (n == 0) "Yok" else "${n}sn") } } }
            HorizontalDivider()
            Text("Video kalitesi")
            SingleChoiceSegmentedButtonRow { listOf("HD", "FHD", "UHD").forEachIndexed { i, q -> SegmentedButton(selected = settings.videoQuality == q, onClick = { onSettingsChange(settings.copy(videoQuality = q)) }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(if (q == "UHD") "4K" else if (q == "FHD") "1080p" else "720p") } } }
            Text("Ayarlar otomatik olarak cihazda saklanır. Metinler ve videolar hesabınıza gönderilmez.")
            Text("Sürüm 1.0.0-beta01", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun estimatedMinutes(text: String): Int = ((text.trim().split(Regex("\\s+")).size / 130f).coerceAtLeast(1f)).roundToInt()
