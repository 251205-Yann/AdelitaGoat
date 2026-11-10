package mx.edu.upchiapas.adelitagoat.data

import android.content.Context
import android.content.SharedPreferences
import mx.edu.upchiapas.adelitagoat.BuildConfig
import mx.edu.upchiapas.adelitagoat.domain.AuthResponse
import mx.edu.upchiapas.adelitagoat.domain.UserDto
import mx.edu.upchiapas.adelitagoat.domain.UserRole

class SessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("adelitagoat_session_prefs", Context.MODE_PRIVATE)

    fun saveSession(auth: AuthResponse) {
        prefs.edit()
            .putString(KEY_TOKEN, auth.token)
            .putString(KEY_EXPIRES_AT, auth.expiresAt)
            .putLong(KEY_USER_ID, auth.user.id)
            .putString(KEY_USER_NAME, auth.user.name)
            .putString(KEY_USER_EMAIL, auth.user.email)
            .putString(KEY_USER_PHONE, auth.user.phone)
            .putString(KEY_USER_ROLE, auth.user.role.name)
            .putBoolean(KEY_ONBOARDING_SEEN, true)
            .apply()
    }

    fun updateUser(user: UserDto) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHONE, user.phone)
            .putString(KEY_USER_ROLE, user.role.name)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getSavedUser(): UserDto? {
        val token = getToken() ?: return null
        val id = prefs.getLong(KEY_USER_ID, -1L)
        val name = prefs.getString(KEY_USER_NAME, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val roleStr = prefs.getString(KEY_USER_ROLE, null) ?: return null
        if (id <= 0L || token.isBlank()) return null
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull() ?: return null
        return UserDto(
            id = id,
            name = name,
            email = email,
            phone = prefs.getString(KEY_USER_PHONE, null),
            role = role
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .remove(KEY_USER_ROLE)
            .apply()
    }

    fun isOnboardingSeen(): Boolean = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)

    fun setOnboardingSeen() {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, BuildConfig.DEFAULT_BASE_URL)
            ?.trim()
            ?.removeSuffix("/")
            ?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        val clean = url.trim().removeSuffix("/")
        if (clean.isNotEmpty()) {
            prefs.edit().putString(KEY_BASE_URL, clean).apply()
        }
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_BASE_URL = "api_base_url"
    }
}
