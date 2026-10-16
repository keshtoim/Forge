package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val version: Int = 1,
    val exercises: List<Exercise>,
    val templates: List<Template>,
    val templateExercises: List<TemplateExercise>,
    val workouts: List<Workout>,
    val workoutExercises: List<WorkoutExercise>,
    val workoutSets: List<WorkoutSet>,
    val plannedWorkouts: List<PlannedWorkout> = emptyList(),
    // Serialized under its old name so backups made before measurements existed still restore their weigh-ins.
    @SerialName("bodyWeights") val bodyMeasurements: List<BodyMeasurement> = emptyList(),
)

@Dao
interface BackupDao {
    @Query("SELECT * FROM exercises") suspend fun exercises(): List<Exercise>
    @Query("SELECT * FROM templates") suspend fun templates(): List<Template>
    @Query("SELECT * FROM template_exercises") suspend fun templateExercises(): List<TemplateExercise>
    @Query("SELECT * FROM workouts") suspend fun workouts(): List<Workout>
    @Query("SELECT * FROM workout_exercises") suspend fun workoutExercises(): List<WorkoutExercise>
    @Query("SELECT * FROM workout_sets") suspend fun workoutSets(): List<WorkoutSet>
    @Query("SELECT * FROM planned_workouts") suspend fun plannedWorkouts(): List<PlannedWorkout>
    @Query("SELECT * FROM body_measurements") suspend fun bodyMeasurements(): List<BodyMeasurement>

    @Query("DELETE FROM planned_workouts") suspend fun deletePlannedWorkouts()
    @Query("DELETE FROM body_measurements") suspend fun deleteBodyMeasurements()
    @Query("DELETE FROM workout_sets") suspend fun deleteWorkoutSets()
    @Query("DELETE FROM workout_exercises") suspend fun deleteWorkoutExercises()
    @Query("DELETE FROM workouts") suspend fun deleteWorkouts()
    @Query("DELETE FROM template_exercises") suspend fun deleteTemplateExercises()
    @Query("DELETE FROM templates") suspend fun deleteTemplates()
    @Query("DELETE FROM exercises") suspend fun deleteExercises()

    @Insert suspend fun insertExercises(items: List<Exercise>)
    @Insert suspend fun insertTemplates(items: List<Template>)
    @Insert suspend fun insertTemplateExercises(items: List<TemplateExercise>)
    @Insert suspend fun insertWorkouts(items: List<Workout>)
    @Insert suspend fun insertWorkoutExercises(items: List<WorkoutExercise>)
    @Insert suspend fun insertWorkoutSets(items: List<WorkoutSet>)
    @Insert suspend fun insertPlannedWorkouts(items: List<PlannedWorkout>)
    @Insert suspend fun insertBodyMeasurements(items: List<BodyMeasurement>)

    @Transaction
    suspend fun export() = Backup(
        exercises = exercises(),
        templates = templates(),
        templateExercises = templateExercises(),
        workouts = workouts(),
        workoutExercises = workoutExercises(),
        workoutSets = workoutSets(),
        plannedWorkouts = plannedWorkouts(),
        bodyMeasurements = bodyMeasurements(),
    )

    // Ids are kept as-is so foreign keys inside the backup stay valid.
    @Transaction
    suspend fun replaceAll(backup: Backup) {
        deletePlannedWorkouts()
        deleteBodyMeasurements()
        deleteWorkoutSets()
        deleteWorkoutExercises()
        deleteWorkouts()
        deleteTemplateExercises()
        deleteTemplates()
        deleteExercises()
        insertExercises(backup.exercises)
        insertTemplates(backup.templates)
        insertTemplateExercises(backup.templateExercises)
        insertWorkouts(backup.workouts)
        insertWorkoutExercises(backup.workoutExercises)
        insertWorkoutSets(backup.workoutSets)
        insertPlannedWorkouts(backup.plannedWorkouts)
        insertBodyMeasurements(backup.bodyMeasurements)
    }
}
