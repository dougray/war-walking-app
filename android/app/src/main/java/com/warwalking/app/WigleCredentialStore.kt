package com.warwalking.app

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted on-device storage for the researcher's real WiGLE API Name/Token.
 * There's no separate WarWalker account anymore - the WiGLE account itself
 * is the identity - so this replaces the old numeric backend user_id store
 * with the actual credentials, which is why it's worth the extra step of
 * EncryptedSharedPreferences instead of plain prefs.
 */
object WigleCredentialStore {
    private const val PREFS_FILE = "wigle_credentials"
    private const val KEY_API_NAME = "api_name"
    private const val KEY_API_TOKEN = "api_token"
    private const val KEY_USERNAME = "username" // cached from the profile check, display only

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(context: Context, apiName: String, apiToken: String, username: String?) {
        prefs(context).edit()
            .putString(KEY_API_NAME, apiName)
            .putString(KEY_API_TOKEN, apiToken)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getApiName(context: Context): String? = prefs(context).getString(KEY_API_NAME, null)

    fun getApiToken(context: Context): String? = prefs(context).getString(KEY_API_TOKEN, null)

    fun getUsername(context: Context): String? = prefs(context).getString(KEY_USERNAME, null)

    fun hasCredentials(context: Context): Boolean =
        !getApiName(context).isNullOrBlank() && !getApiToken(context).isNullOrBlank()

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
