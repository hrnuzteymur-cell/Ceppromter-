package com.cepprompter.app.data

import android.content.Context
import com.cepprompter.app.model.Script
import com.cepprompter.app.model.PrompterSettings
import org.json.JSONArray
import org.json.JSONObject

class ScriptRepository(context: Context) {
    private val prefs = context.getSharedPreferences("cep_prompter", Context.MODE_PRIVATE)

    fun load(): List<Script> {
        val raw = prefs.getString(KEY, null) ?: return listOf(welcomeScript())
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                Script(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    body = item.getString("body"),
                    favorite = item.optBoolean("favorite"),
                    folder = item.optString("folder", "Genel"),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }.ifEmpty { listOf(welcomeScript()) }
        }.getOrElse { listOf(welcomeScript()) }
    }

    fun save(scripts: List<Script>) {
        val array = JSONArray()
        scripts.forEach { script ->
            array.put(JSONObject().apply {
                put("id", script.id)
                put("title", script.title)
                put("body", script.body)
                put("favorite", script.favorite)
                put("folder", script.folder)
                put("updatedAt", script.updatedAt)
            })
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun loadSettings(): PrompterSettings = PrompterSettings(
        speed = prefs.getFloat("speed", .42f),
        fontSize = prefs.getFloat("fontSize", 34f),
        lineSpacing = prefs.getFloat("lineSpacing", 1.25f),
        panelWidth = prefs.getFloat("panelWidth", .88f),
        panelOpacity = prefs.getFloat("panelOpacity", .66f),
        countdown = prefs.getInt("countdown", 3),
        mirrorHorizontal = prefs.getBoolean("mirrorHorizontal", false),
        mirrorVertical = prefs.getBoolean("mirrorVertical", false),
        loop = prefs.getBoolean("loop", false),
        highlightCenter = prefs.getBoolean("highlightCenter", true),
        timedMinutes = prefs.getInt("timedMinutes", 0).takeIf { it > 0 },
        textColor = prefs.getLong("textColor", 0xFFFFFFFF),
        readingPosition = prefs.getString("readingPosition", "CENTER") ?: "CENTER",
        videoQuality = prefs.getString("videoQuality", "FHD") ?: "FHD"
    )

    fun saveSettings(settings: PrompterSettings) {
        prefs.edit()
            .putFloat("speed", settings.speed)
            .putFloat("fontSize", settings.fontSize)
            .putFloat("lineSpacing", settings.lineSpacing)
            .putFloat("panelWidth", settings.panelWidth)
            .putFloat("panelOpacity", settings.panelOpacity)
            .putInt("countdown", settings.countdown)
            .putBoolean("mirrorHorizontal", settings.mirrorHorizontal)
            .putBoolean("mirrorVertical", settings.mirrorVertical)
            .putBoolean("loop", settings.loop)
            .putBoolean("highlightCenter", settings.highlightCenter)
            .putInt("timedMinutes", settings.timedMinutes ?: 0)
            .putLong("textColor", settings.textColor)
            .putString("readingPosition", settings.readingPosition)
            .putString("videoQuality", settings.videoQuality)
            .apply()
    }

    fun loadProgress(scriptId: String): Float = prefs.getFloat("progress_$scriptId", 0f)

    fun saveProgress(scriptId: String, progress: Float) {
        prefs.edit().putFloat("progress_$scriptId", progress.coerceIn(0f, 1f)).apply()
    }

    private fun welcomeScript() = Script(
        title = "Cep Prompter’a Hoş Geldiniz",
        body = "Merhaba. Bu, Cep Prompter için hazırlanmış örnek metindir. " +
            "Oynat düğmesine dokunduğunuzda metin seçtiğiniz hızda akmaya başlar. " +
            "Yazı boyutunu, hızı ve okuma alanını çekim sırasında değiştirebilirsiniz. " +
            "Kamera modunda ön veya arka kamerayı kullanarak videonuzu kaydedebilirsiniz."
    )

    companion object { private const val KEY = "scripts_v1" }
}
