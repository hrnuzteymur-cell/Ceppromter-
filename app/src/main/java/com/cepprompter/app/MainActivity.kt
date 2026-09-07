package com.cepprompter.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.cepprompter.app.ui.CepPrompterApp
import com.cepprompter.app.ui.CepPrompterTheme

class MainActivity : ComponentActivity() {
    private var incomingText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        incomingText = extractSharedText(intent)
        setContent {
            CepPrompterTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        CepPrompterApp(incomingText = incomingText) { incomingText = null }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingText = extractSharedText(intent)
    }

    private fun extractSharedText(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }
}
