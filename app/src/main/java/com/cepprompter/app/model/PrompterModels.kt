package com.cepprompter.app.model

import java.util.UUID

data class Script(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val favorite: Boolean = false,
    val folder: String = "Genel",
    val updatedAt: Long = System.currentTimeMillis()
)

data class PrompterSettings(
    val speed: Float = 0.42f,
    val fontSize: Float = 34f,
    val lineSpacing: Float = 1.25f,
    val panelWidth: Float = 0.88f,
    val panelOpacity: Float = 0.66f,
    val countdown: Int = 3,
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val loop: Boolean = false,
    val highlightCenter: Boolean = true,
    val timedMinutes: Int? = null,
    val textColor: Long = 0xFFFFFFFF,
    val readingPosition: String = "CENTER",
    val videoQuality: String = "FHD",
    val resumeProgress: Float = 0f
)

enum class AppPage { LIBRARY, EDITOR, PROMPTER, CAMERA, SETTINGS }
