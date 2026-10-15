package com.keshtoim.forge

import android.app.Application
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.db.ForgeDatabase
import com.keshtoim.forge.reminder.PlanReminders
import com.keshtoim.forge.rest.RestTimer
import com.keshtoim.forge.rest.RestTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ForgeApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { ForgeDatabase.build(this) }
    val restTimer by lazy { RestTimer(this) }
    val preferences by lazy { UserPreferences(this) }
    val planReminders by lazy { PlanReminders(this) }

    override fun onCreate() {
        super.onCreate()
        RestTimerService.createChannels(this)
        PlanReminders.createChannel(this)
        appScope.launch { database.exerciseDao().seedBuiltIns() }
        // Alarms are dropped by a force stop, and plans may come from a restored backup, so every start re-arms them.
        appScope.launch { database.planDao().getAll().forEach(planReminders::schedule) }
    }
}
