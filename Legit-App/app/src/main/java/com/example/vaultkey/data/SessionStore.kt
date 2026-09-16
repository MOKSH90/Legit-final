package com.example.vaultkey.data

import android.content.Context

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("vaultkey_session", Context.MODE_PRIVATE)

    fun read(): SessionData? {
        val token = preferences.getString("token", null) ?: return null
        val refreshToken = preferences.getString("refresh_token", null) ?: return null
        val userId = preferences.getString("user_id", null) ?: return null
        val username = preferences.getString("username", null) ?: return null
        val legitId = preferences.getString("legit_id", null)
        val role = preferences.getString("role", null) ?: return null
        return SessionData(token, refreshToken, userId, username, legitId, role)
    }

    fun save(session: SessionData) {
        preferences.edit()
            .putString("token", session.token)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId)
            .putString("username", session.username)
            .putString("legit_id", session.legitId)
            .putString("role", session.role)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
