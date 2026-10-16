package com.keshtoim.forge.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    QUADS, HAMSTRINGS, GLUTES, CALVES, ABS, CARDIO, OTHER,
}

enum class ExerciseType { WEIGHT_REPS, REPS, DURATION, DISTANCE }

// Persisted by name: never rename entries.
enum class SetEffort { WARMUP, LIGHT, MEDIUM, HEAVY, MAX }

// Exercise characteristics, persisted by name: never rename entries.
enum class Mechanics { COMPOUND, ISOLATION, OTHER }

enum class Discipline { STRENGTH, CARDIO, STRETCHING, PLYOMETRIC, POWERLIFTING, WEIGHTLIFTING }

enum class Equipment { NONE, BARBELL, DUMBBELL, KETTLEBELL, MACHINE, CABLE, BAND, FITBALL, OTHER }

enum class Force { PUSH, PULL, STATIC, OTHER }

@Serializable
@Entity(tableName = "exercises", indices = [Index(value = ["builtInKey"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Built-in exercises take their name and secondary muscles from BuiltInExercise, custom ones from these columns.
    val builtInKey: String? = null,
    val name: String? = null,
    val muscleGroup: MuscleGroup,
    @ColumnInfo(defaultValue = "")
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val type: ExerciseType,
    val archived: Boolean = false,
    // Weight of the bar when sets are entered as plates per side; null means the total weight is entered.
    val barKg: Double? = null,
    // Characteristics of custom exercises; built-ins take theirs from BuiltInExercise.traits.
    val mechanics: Mechanics? = null,
    val discipline: Discipline? = null,
    val equipment: Equipment? = null,
    val force: Force? = null,
)

@Serializable
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Serializable
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

@Serializable
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

@Serializable
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

@Serializable
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
    val effort: SetEffort? = null,
)

@Serializable
@Entity(
    tableName = "planned_workouts",
    foreignKeys = [ForeignKey(Template::class, ["id"], ["templateId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("templateId")],
)
data class PlannedWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    // The only day of a one-off plan, or the first day of a repeating one.
    val epochDay: Long,
    val minuteOfDay: Int,
    // Bit n is set when the plan repeats on DayOfWeek.of(n + 1); 0 means a one-off plan.
    val repeatDays: Int = 0,
    val remindBefore: Int? = null,
)

@Serializable
@Entity(tableName = "body_measurements", indices = [Index(value = ["epochDay"], unique = true)])
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // One entry per day: values logged later the same day are merged into it.
    val epochDay: Long,
    val weightKg: Double? = null,
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val bicepsCm: Double? = null,
    val thighCm: Double? = null,
    val calfCm: Double? = null,
)

// Warm-up sets are logged but left out of volume, records and statistics.
val WorkoutSet.isWorking: Boolean get() = completed && effort != SetEffort.WARMUP
