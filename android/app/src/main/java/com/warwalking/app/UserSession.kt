package com.warwalking.app

import android.content.Context

/** Minimal local persistence for the logged-in researcher's backend user_id. */
object UserSession {
    private const val PREFS = "war_walking_prefs"
    private const val KEY_USER_ID = "user_id"

    fun save(context: Context, userId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): Int? {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_USER_ID, -1)
        return id.takeIf { it != -1 }
    }
}
