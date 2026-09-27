package com.craysafe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.craysafe.notifications.NotificationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Pass the fragments IDs to the AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_alerts,
                R.id.navigation_reports,
                R.id.navigation_support
            )
        )

        // Setup action bar with nav controller
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup bottom nav
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
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
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