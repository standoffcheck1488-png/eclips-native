package com.eclips.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var configInput: EditText
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configInput = findViewById(R.id.configInput)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val saved = prefs.getString("config", "") ?: ""
        if (saved.isNotEmpty()) configInput.setText(saved)

        saveButton.setOnClickListener {
            prefs.edit().putString("config", configInput.text.toString()).apply()
            statusText.text = "Конфиг сохранён"
        }

        connectButton.setOnClickListener {
            if (connected) {
                stopVpn()
            } else {
                val prepare = VpnService.prepare(this)
                if (prepare != null) {
                    startActivityForResult(prepare, 100)
                } else {
                    startVpn()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startVpn()
        } else {
            statusText.text = "Разрешение VPN не выдано"
        }
    }

    private fun startVpn() {
        val config = configInput.text.toString()
        if (!config.contains("[Interface]") || !config.contains("[Peer]")) {
            statusText.text = "Неверный формат конфига"
            return
        }
        val i = Intent(this, TunnelService::class.java)
        i.action = "START"
        i.putExtra("config", config)
        startService(i)
        connected = true
        statusText.text = "Подключено 🌑"
        connectButton.text = "Отключить"
    }

    private fun stopVpn() {
        val i = Intent(this, TunnelService::class.java)
        i.action = "STOP"
        startService(i)
        connected = false
        statusText.text = "Не подключено"
        connectButton.text = "Подключить"
    }
}
