package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExercise,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSet>,
)

data class WorkoutDetail(
    @Embedded val workout: Workout,
    @Relation(entity = WorkoutExercise::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithSets>,
)

data class ExerciseHistory(val exerciseId: Long, val lastAt: Long, val sessions: Int)

data class SetPoint(
    val workoutId: Long,
    val startedAt: Long,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
)

@Dao
interface WorkoutDao {
    @Query(
        """
        SELECT we.exerciseId AS exerciseId, MAX(w.startedAt) AS lastAt, COUNT(DISTINCT w.id) AS sessions
        FROM workout_exercises we JOIN workouts w ON w.id = we.workoutId
        WHERE w.finishedAt IS NOT NULL
        GROUP BY we.exerciseId ORDER BY lastAt DESC
        """
    )
    fun observeExerciseHistory(): Flow<List<ExerciseHistory>>

    @Query(
        """
        SELECT w.id AS workoutId, w.startedAt, s.weightKg, s.reps, s.durationSec, s.distanceM
        FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.completed = 1 AND w.finishedAt IS NOT NULL
        ORDER BY w.startedAt
        """
    )
    fun observeSetPoints(exerciseId: Long): Flow<List<SetPoint>>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL LIMIT 1")
    fun observeActive(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getActive(): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeDetail(id: Long): Flow<WorkoutDetail?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<WorkoutDetail>>

    @Query(
        """
        SELECT * FROM workout_sets WHERE workoutExerciseId = (
            SELECT we.id FROM workout_exercises we JOIN workouts w ON w.id = we.workoutId
            WHERE we.exerciseId = :exerciseId AND w.finishedAt IS NOT NULL
            ORDER BY w.startedAt DESC, we.position LIMIT 1
        ) ORDER BY position
        """
    )
    suspend fun previousSets(exerciseId: Long): List<WorkoutSet>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getSet(id: Long): WorkoutSet?

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY position DESC LIMIT 1")
    suspend fun lastSet(workoutExerciseId: Long): WorkoutSet?

    @Insert
    suspend fun insert(workout: Workout): Long

    @Insert
    suspend fun insert(workoutExercise: WorkoutExercise): Long

    @Insert
    suspend fun insert(set: WorkoutSet): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Update
    suspend fun update(set: WorkoutSet)

    @Delete
    suspend fun delete(set: WorkoutSet)

    @Delete
    suspend fun delete(workoutExercise: WorkoutExercise)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun nextExercisePosition(workoutId: Long): Int

    @Query("DELETE FROM workout_sets WHERE completed = 0 AND workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)")
    suspend fun deleteIncompleteSets(workoutId: Long)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId AND id NOT IN (SELECT workoutExerciseId FROM workout_sets)")
    suspend fun deleteEmptyExercises(workoutId: Long)

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun countExercises(workoutId: Long): Int

    @Query("UPDATE workouts SET finishedAt = :finishedAt WHERE id = :workoutId")
    suspend fun setFinished(workoutId: Long, finishedAt: Long)

    @Transaction
    suspend fun start(name: String, templateId: Long?, items: List<Pair<Long, Int>>, now: Long): Long {
        val workoutId = insert(Workout(templateId = templateId, name = name, startedAt = now))
        items.forEachIndexed { i, (exerciseId, sets) ->
            val weId = insert(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = i))
            insertSets(List(sets) { WorkoutSet(workoutExerciseId = weId, position = it) })
        }
        return workoutId
    }

    // Only one workout may be in progress; checking and inserting in one transaction prevents two from racing in.
    @Transaction
    suspend fun startOrResume(name: String, templateId: Long?, items: List<Pair<Long, Int>>, now: Long): Long =
        getActive()?.id ?: start(name, templateId, items, now)

    @Transaction
    suspend fun addExercises(workoutId: Long, exerciseIds: List<Long>) {
        var position = nextExercisePosition(workoutId)
        exerciseIds.forEach { exerciseId ->
            val weId = insert(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = position++))
            insert(WorkoutSet(workoutExerciseId = weId, position = 0))
        }
    }

    // Returns false when nothing was completed and the workout was dropped entirely.
    @Transaction
    suspend fun finish(workoutId: Long, now: Long): Boolean {
        deleteIncompleteSets(workoutId)
        deleteEmptyExercises(workoutId)
        if (countExercises(workoutId) == 0) {
            deleteWorkout(workoutId)
            return false
        }
        setFinished(workoutId, now)
        return true
    }
}
