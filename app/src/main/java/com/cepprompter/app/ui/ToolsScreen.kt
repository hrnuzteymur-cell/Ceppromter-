@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cepprompter.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cepprompter.app.model.Script
import com.cepprompter.app.overlay.OverlayPrompterService
import com.cepprompter.app.remote.RemoteControlService
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun ToolsScreen(script: Script, onVideoEditor: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var overlayRunning by remember { mutableStateOf(false) }
    var remoteRunning by remember { mutableStateOf(false) }
    val overlayPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(context, Intent(context, OverlayPrompterService::class.java).putExtra(OverlayPrompterService.EXTRA_TEXT, script.body))
            overlayRunning = true
        }
    }
    fun toggleOverlay() {
        if (overlayRunning) {
            context.stopService(Intent(context, OverlayPrompterService::class.java)); overlayRunning = false
        } else if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Kayan pencere için 'diğer uygulamaların üzerinde göster' iznini açın", Toast.LENGTH_LONG).show()
            overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        } else {
            ContextCompat.startForegroundService(context, Intent(context, OverlayPrompterService::class.java).putExtra(OverlayPrompterService.EXTRA_TEXT, script.body)); overlayRunning = true
        }
    }
    fun toggleRemote() {
        if (remoteRunning) context.stopService(Intent(context, RemoteControlService::class.java))
        else ContextCompat.startForegroundService(context, Intent(context, RemoteControlService::class.java).putExtra(RemoteControlService.EXTRA_TEXT, script.body))
        remoteRunning = !remoteRunning
    }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Araçlar ve bağlantılar") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolCard(Icons.Default.PictureInPicture, "Kayan pencere", "Seçili metni kamera, canlı yayın veya görüşme uygulamasının üzerinde taşınabilir ve boyutlandırılabilir pencerede açar.", if (overlayRunning) "Durdur" else "Başlat", ::toggleOverlay)
            ToolCard(Icons.Default.Wifi, "Aynı Wi‑Fi kumandası", if (remoteRunning) "İkinci cihazın tarayıcısında http://${localIpv4()}:${RemoteControlService.PORT} adresini açın. Metin önizleme, oynat/durdur, hız ve konum kontrolü aktiftir." else "Telefon ve kumanda cihazı aynı Wi‑Fi ağına bağlı olmalıdır.", if (remoteRunning) "Sunucuyu kapat" else "Sunucuyu başlat", ::toggleRemote)
            ToolCard(Icons.Default.Bluetooth, "Bluetooth ve USB kumandalar", "Klavye, sunum kumandası, pedal ve oyun kolu eşleştirildiğinde: Space/Enter/A oynatır; ses veya yön tuşları ve L1/R1 hızı değiştirir; Esc/B durdurur.", "Android bağlantı ayarları", {
                runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }.onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            })
            ToolCard(Icons.Default.Watch, "Wear OS kumandası", "Cep Prompter saat modülü, Google Play Hizmetleri üzerinden telefona oynat/durdur ve hız komutlarını iletir. Saat uygulamasını aynı projeden yükleyin.", "Saat modülü hazır", {})
            ToolCard(Icons.Default.Movie, "Video düzenleme stüdyosu", "Çekimi seçin; kırpma/döndürme, en-boy oranı, altyazı, logo-yazı ve paylaşım çıktılarını hazırlayın.", "Stüdyoyu aç", onVideoEditor)
            Text("Gizlilik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Wi‑Fi kumandası yalnızca yerel ağda çalışır; internet sunucusuna metin göndermez. Kayan pencere ve kumanda servisleri siz kapatana kadar bildirim gösterir.")
        }
    }
}

@Composable
private fun ToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, button: String, action: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        Text(body)
        Button(onClick = action, enabled = button != "Saat modülü hazır") { Text(button) }
    } }
}

private fun localIpv4(): String = runCatching {
    NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress ?: "telefon-ip"
}.getOrDefault("telefon-ip")
