package com.keshtoim.forge.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    QUADS, HAMSTRINGS, GLUTES, CALVES, ABS, CARDIO, OTHER,
}

enum class ExerciseType { WEIGHT_REPS, REPS, DURATION, DISTANCE }

@Entity(tableName = "exercises", indices = [Index(value = ["builtInKey"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Built-in exercises are named via string resources (see BuiltInExercise), custom ones via [name].
    val builtInKey: String? = null,
    val name: String? = null,
    val muscleGroup: MuscleGroup,
    val type: ExerciseType,
    val archived: Boolean = false,
)

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(Template::class, ["id"], ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
)

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(Template::class, ["id"], ["templateId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("templateId"), Index("startedAt")],
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long? = null,
    val name: String,
    val startedAt: Long,
    // null while the workout is in progress; used to restore it after process death.
    val finishedAt: Long? = null,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(Workout::class, ["id"], ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(WorkoutExercise::class, ["id"], ["workoutExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val distanceM: Double? = null,
    val completed: Boolean = false,
)
