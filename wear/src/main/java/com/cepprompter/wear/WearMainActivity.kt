package com.cepprompter.wear

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.wearable.Wearable

class WearMainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(18, 18, 18, 18); setBackgroundColor(Color.rgb(4, 12, 18))
        }
        layout.addView(TextView(this).apply { text = "Cep Prompter"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        layout.addView(Button(this).apply { text = "− Hız"; setOnClickListener { send("/cep/slower") } })
        layout.addView(Button(this).apply { text = "▶  /  Ⅱ"; textSize = 22f; setOnClickListener { send("/cep/toggle") } })
        layout.addView(Button(this).apply { text = "+ Hız"; setOnClickListener { send("/cep/faster") } })
        setContentView(layout)
    }
    private fun send(path: String) {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) Toast.makeText(this, "Telefon bağlı değil", Toast.LENGTH_SHORT).show()
            nodes.forEach { Wearable.getMessageClient(this).sendMessage(it.id, path, byteArrayOf()) }
        }.addOnFailureListener { Toast.makeText(this, "Komut gönderilemedi", Toast.LENGTH_SHORT).show() }
    }
}
