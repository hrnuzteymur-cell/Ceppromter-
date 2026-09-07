package com.cepprompter.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.cepprompter.app.ui.CepPrompterApp
import com.cepprompter.app.ui.CepPrompterTheme
import com.cepprompter.app.control.ControlCenter
import com.cepprompter.app.control.PrompterCommand

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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return super.dispatchKeyEvent(event)
        val command = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_BUTTON_R1 -> PrompterCommand.Faster
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BUTTON_L1 -> PrompterCommand.Slower
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_HEADSETHOOK -> PrompterCommand.Toggle
            KeyEvent.KEYCODE_MEDIA_PLAY -> PrompterCommand.Play
            KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_B -> PrompterCommand.Pause
            else -> null
        }
        return if (command != null) { ControlCenter.send(command); true } else super.dispatchKeyEvent(event)
    }

    private fun extractSharedText(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }
}
