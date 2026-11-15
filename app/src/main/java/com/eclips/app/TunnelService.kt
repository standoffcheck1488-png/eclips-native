package com.eclips.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class TunnelService : VpnService() {

    companion object {
        const val TAG = "EclipsVPN"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                val config = intent.getStringExtra("config") ?: ""
                startVpn(config)
            }
            "STOP" -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(config: String) {
        try {
            val builder = Builder()
            builder.setSession("Eclips VPN")
            builder.setMtu(1420)
            builder.addAddress("10.0.0.2", 32)
            builder.addDnsServer("1.1.1.1")
            builder.addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            isRunning = true

            Log.d(TAG, "VPN tunnel established")
            
            // Запускаем поток для обработки трафика
            Thread {
                processTraffic()
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopSelf()
        }
    }

    private fun processTraffic() {
        val vpnInterface = vpnInterface ?: return
        val inputStream = FileInputStream(vpnInterface.fileDescriptor)
        val outputStream = FileOutputStream(vpnInterface.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        while (isRunning) {
            try {
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    // Здесь должна быть логика шифрования WireGuard
                    // Для демо просто пересылаем пакеты
                    buffer.limit(length)
                    outputStream.write(buffer.array(), 0, length)
                    buffer.clear()
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error processing traffic", e)
                }
                break
            }
        }
    }

    private fun stopVpn() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}
