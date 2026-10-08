package com.keshtoim.forge

import android.app.Application
import com.keshtoim.forge.data.BuiltInExercise
import com.keshtoim.forge.data.db.ForgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ForgeApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { ForgeDatabase.build(this) }

    override fun onCreate() {
        super.onCreate()
        // Idempotent: unique builtInKey makes this add only exercises introduced in newer app versions.
        appScope.launch {
            database.exerciseDao().insertIgnore(BuiltInExercise.entries.map { it.toEntity() })
        }
    }
}
