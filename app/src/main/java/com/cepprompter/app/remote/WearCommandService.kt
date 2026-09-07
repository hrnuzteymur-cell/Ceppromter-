package com.cepprompter.app.remote

import com.cepprompter.app.control.ControlCenter
import com.cepprompter.app.control.PrompterCommand
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearCommandService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val command = when (event.path) {
            "/cep/toggle" -> PrompterCommand.Toggle
            "/cep/faster" -> PrompterCommand.Faster
            "/cep/slower" -> PrompterCommand.Slower
            "/cep/play" -> PrompterCommand.Play
            "/cep/pause" -> PrompterCommand.Pause
            else -> null
        }
        command?.let(ControlCenter::send)
    }
}
