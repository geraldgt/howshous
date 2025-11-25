package io.github.howshous.ui.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "howshous_prefs")

object SessionKeys {
    val ROLE = stringPreferencesKey("role")
    val UID = stringPreferencesKey("uid")
    val WELCOME_SHOWN = booleanPreferencesKey("welcome_shown")
}

suspend fun saveRole(context: Context, role: String) {
    context.dataStore.edit { it[SessionKeys.ROLE] = role }
}

suspend fun saveUid(context: Context, uid: String) {
    context.dataStore.edit { it[SessionKeys.UID] = uid }
}

suspend fun setWelcomeShown(context: Context) {
    context.dataStore.edit { it[SessionKeys.WELCOME_SHOWN] = true }
}

fun readRoleFlow(context: Context): Flow<String> =
    context.dataStore.data.map { it[SessionKeys.ROLE] ?: "" }

fun readUidFlow(context: Context): Flow<String> =
    context.dataStore.data.map { it[SessionKeys.UID] ?: "" }

fun readWelcomeShownFlow(context: Context): Flow<Boolean> =
    context.dataStore.data.map { it[SessionKeys.WELCOME_SHOWN] ?: false }

suspend fun clearSession(context: Context) {
    context.dataStore.edit { it.clear() }
}