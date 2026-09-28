package com.craysafe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.craysafe.api.ApiClient
import com.craysafe.notifications.NotificationHelper
import com.craysafe.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val sessionManager: SessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        NotificationHelper.createChannels(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val badgeReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                refreshAlertsBadge()
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            badgeReceiver,
            android.content.IntentFilter("com.craysafe.REFRESH_BADGE"),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStart() {
        super.onStart()
        refreshAlertsBadge()
    }

    fun refreshAlertsBadge() {
        if (!sessionManager.isLoggedIn()) return
        val token = sessionManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getUnreadAlertCount("Bearer $token")
                if (response.success) {
                    updateAlertsBadge(response.count)
                }
            } catch (_: Exception) {

            }
        }
    }

    fun updateAlertsBadge(count: Int) {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val badge = navView.getOrCreateBadge(R.id.navigation_alerts)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
            badge.backgroundColor = android.graphics.Color.RED
        } else {
            badge.isVisible = false
        }
    }
}