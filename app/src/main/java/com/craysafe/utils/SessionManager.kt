package com.craysafe.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("craysafe_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_SELECTED_TANK_ID = "selected_tank_id"
    }

    fun saveAuth(token: String, userId: Int, username: String, email: String, role: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, "user")
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun setSelectedTankId(tankId: Int) {
        prefs.edit().putInt(KEY_SELECTED_TANK_ID, tankId).apply()
    }
    fun getSelectedTankId(): Int {
        return prefs.getInt(KEY_SELECTED_TANK_ID, -1)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}