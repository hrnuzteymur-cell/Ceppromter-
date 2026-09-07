package com.cepprompter.app.control

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface PrompterCommand {
    data object Toggle : PrompterCommand
    data object Play : PrompterCommand
    data object Pause : PrompterCommand
    data object Faster : PrompterCommand
    data object Slower : PrompterCommand
    data class Seek(val progress: Float) : PrompterCommand
}

object ControlCenter {
    private val mutableCommands = MutableSharedFlow<PrompterCommand>(extraBufferCapacity = 32)
    val commands = mutableCommands.asSharedFlow()
    fun send(command: PrompterCommand) { mutableCommands.tryEmit(command) }
}
