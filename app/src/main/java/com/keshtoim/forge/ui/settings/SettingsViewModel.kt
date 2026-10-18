package com.keshtoim.forge.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.ThemeMode
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.ai.AiSettings
import com.keshtoim.forge.data.db.Backup
import com.keshtoim.forge.data.db.BackupDao
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.reminder.PlanReminders
import com.keshtoim.forge.rest.RestTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val backupDao: BackupDao,
    private val exerciseDao: ExerciseDao,
    private val contentResolver: ContentResolver,
    private val restTimer: RestTimer,
    private val preferences: UserPreferences,
    private val reminders: PlanReminders,
    aiSettings: AiSettings,
) : ViewModel() {
    val ai = aiSettings.state

    val bodyType = preferences.bodyType

    fun setBodyType(type: BodyType) = preferences.setBodyType(type)

    val themeMode = preferences.themeMode

    fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    private val json = Json { ignoreUnknownKeys = true }

    fun export(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = json.encodeToString(Backup.serializer(), backupDao.export())
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(text) }
                }
            }.isSuccess
            onResult(ok)
        }
    }

    fun import(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                val backup = json.decodeFromString(Backup.serializer(), text)
                restTimer.stop()
                backupDao.plannedWorkouts().forEach { reminders.cancel(it.id) }
                backupDao.replaceAll(backup)
                backup.plannedWorkouts.forEach(reminders::schedule)
                // An older backup lacks built-in exercises added since it was made.
                exerciseDao.seedBuiltIns()
            }.isSuccess
            onResult(ok)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                SettingsViewModel(app.database.backupDao(), app.database.exerciseDao(), app.contentResolver, app.restTimer, app.preferences, app.planReminders, app.aiSettings)
            }
        }
    }
}
