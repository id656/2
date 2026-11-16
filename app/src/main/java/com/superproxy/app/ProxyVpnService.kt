package com.superproxy.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import com.github.shadowsocks.bg.Tun2proxy

class ProxyVpnService : VpnService() {
    companion object {
        @Volatile var running = false
        const val ACTION_STOP = "com.superproxy.app.STOP"
        const val EXTRA_URL = "url"
        private const val CHANNEL = "vpn"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopTunnel(); return Service.START_NOT_STICKY }
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url == null || running) { if (!running) stopSelf(); return Service.START_NOT_STICKY }

        startFg()
        val tun = Builder()
            .setSession("SuperProxy")
            .setMtu(1500)
            .addAddress("10.255.0.2", 32)
            .addAddress("fd00:255::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .apply { addDisallowedApplication(packageName) } // соединения самого приложения (к прокси) идут мимо VPN
            .establish()
        if (tun == null) { stopSelf(); return Service.START_NOT_STICKY }

        val fd = tun.detachFd() // дальше fd принадлежит tun2proxy
        running = true
        val args = "tun2proxy-bin --proxy '$url' --tun-fd $fd --close-fd-on-drop true --dns over-tcp --verbosity info"
        Thread {
            try { Tun2proxy.run(args, 1500.toChar()) }
            finally {
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
        return Service.START_NOT_STICKY
    }

    private fun stopTunnel() { if (running) Tun2proxy.stop() else stopSelf() }
    override fun onRevoke() { stopTunnel() }
    override fun onDestroy() { if (running) Tun2proxy.stop(); super.onDestroy() }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "VPN", NotificationManager.IMPORTANCE_LOW))
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("SuperProxy")
            .setContentText("Трафик идёт через прокси")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }
}
