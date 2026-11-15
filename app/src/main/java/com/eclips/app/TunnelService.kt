package com.eclips.app

import android.content.Intent
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.StringReader

class TunnelService : VpnService() {

    private val backend = GoBackend(this)
    private var currentTunnel: Tunnel? = null

    private val tunnel = object : Tunnel {
        override fun getName() = "Eclips"
        override fun onStateChange(newState: Tunnel.State) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> start(intent.getStringExtra("config") ?: "")
            "STOP" -> stop()
        }
        return START_STICKY
    }

    private fun start(configText: String) {
        try {
            val config = Config.parse(StringReader(configText))
            currentTunnel = tunnel
            backend.setState(tunnel, Tunnel.State.UP, config)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stop() {
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentTunnel = null
        stopSelf()
    }

    override fun onRevoke() {
        stop()
        super.onRevoke()
    }
}
