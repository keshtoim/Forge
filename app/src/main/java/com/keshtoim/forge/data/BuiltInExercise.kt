package com.keshtoim.forge.data

import androidx.annotation.StringRes
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.ExerciseType.DISTANCE
import com.keshtoim.forge.data.db.ExerciseType.DURATION
import com.keshtoim.forge.data.db.ExerciseType.REPS
import com.keshtoim.forge.data.db.ExerciseType.WEIGHT_REPS
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.MuscleGroup.ABS
import com.keshtoim.forge.data.db.MuscleGroup.BACK
import com.keshtoim.forge.data.db.MuscleGroup.BICEPS
import com.keshtoim.forge.data.db.MuscleGroup.CALVES
import com.keshtoim.forge.data.db.MuscleGroup.CARDIO
import com.keshtoim.forge.data.db.MuscleGroup.CHEST
import com.keshtoim.forge.data.db.MuscleGroup.FOREARMS
import com.keshtoim.forge.data.db.MuscleGroup.GLUTES
import com.keshtoim.forge.data.db.MuscleGroup.HAMSTRINGS
import com.keshtoim.forge.data.db.MuscleGroup.QUADS
import com.keshtoim.forge.data.db.MuscleGroup.SHOULDERS
import com.keshtoim.forge.data.db.MuscleGroup.TRICEPS

// Enum names are persisted as Exercise.builtInKey: never rename existing entries.
enum class BuiltInExercise(
    @StringRes val nameRes: Int,
    val muscleGroup: MuscleGroup,
    val type: ExerciseType,
    vararg secondary: MuscleGroup,
) {
    BENCH_PRESS(R.string.ex_bench_press, CHEST, WEIGHT_REPS, TRICEPS, SHOULDERS),
    INCLINE_BENCH_PRESS(R.string.ex_incline_bench_press, CHEST, WEIGHT_REPS, SHOULDERS, TRICEPS),
    DECLINE_BENCH_PRESS(R.string.ex_decline_bench_press, CHEST, WEIGHT_REPS, TRICEPS, SHOULDERS),
    DUMBBELL_BENCH_PRESS(R.string.ex_dumbbell_bench_press, CHEST, WEIGHT_REPS, TRICEPS, SHOULDERS),
    INCLINE_DUMBBELL_PRESS(R.string.ex_incline_dumbbell_press, CHEST, WEIGHT_REPS, SHOULDERS, TRICEPS),
    DUMBBELL_FLY(R.string.ex_dumbbell_fly, CHEST, WEIGHT_REPS, SHOULDERS),
    CABLE_CROSSOVER(R.string.ex_cable_crossover, CHEST, WEIGHT_REPS, SHOULDERS),
    CHEST_PRESS_MACHINE(R.string.ex_chest_press_machine, CHEST, WEIGHT_REPS, TRICEPS, SHOULDERS),
    PUSH_UP(R.string.ex_push_up, CHEST, REPS, TRICEPS, SHOULDERS, ABS),

    DEADLIFT(R.string.ex_deadlift, BACK, WEIGHT_REPS, HAMSTRINGS, GLUTES, FOREARMS),
    PULL_UP(R.string.ex_pull_up, BACK, REPS, BICEPS, FOREARMS),
    CHIN_UP(R.string.ex_chin_up, BACK, REPS, BICEPS, FOREARMS),
    LAT_PULLDOWN(R.string.ex_lat_pulldown, BACK, WEIGHT_REPS, BICEPS),
    BARBELL_ROW(R.string.ex_barbell_row, BACK, WEIGHT_REPS, BICEPS, SHOULDERS),
    DUMBBELL_ROW(R.string.ex_dumbbell_row, BACK, WEIGHT_REPS, BICEPS, SHOULDERS),
    SEATED_CABLE_ROW(R.string.ex_seated_cable_row, BACK, WEIGHT_REPS, BICEPS, SHOULDERS),
    T_BAR_ROW(R.string.ex_t_bar_row, BACK, WEIGHT_REPS, BICEPS, SHOULDERS),
    HYPEREXTENSION(R.string.ex_hyperextension, BACK, REPS, GLUTES, HAMSTRINGS),
    SHRUG(R.string.ex_shrug, BACK, WEIGHT_REPS, FOREARMS),

    OVERHEAD_PRESS(R.string.ex_overhead_press, SHOULDERS, WEIGHT_REPS, TRICEPS),
    DUMBBELL_SHOULDER_PRESS(R.string.ex_dumbbell_shoulder_press, SHOULDERS, WEIGHT_REPS, TRICEPS),
    ARNOLD_PRESS(R.string.ex_arnold_press, SHOULDERS, WEIGHT_REPS, TRICEPS),
    LATERAL_RAISE(R.string.ex_lateral_raise, SHOULDERS, WEIGHT_REPS),
    FRONT_RAISE(R.string.ex_front_raise, SHOULDERS, WEIGHT_REPS, CHEST),
    REAR_DELT_FLY(R.string.ex_rear_delt_fly, SHOULDERS, WEIGHT_REPS, BACK),
    FACE_PULL(R.string.ex_face_pull, SHOULDERS, WEIGHT_REPS, BACK),
    UPRIGHT_ROW(R.string.ex_upright_row, SHOULDERS, WEIGHT_REPS, BACK, BICEPS),

    BARBELL_CURL(R.string.ex_barbell_curl, BICEPS, WEIGHT_REPS, FOREARMS),
    DUMBBELL_CURL(R.string.ex_dumbbell_curl, BICEPS, WEIGHT_REPS, FOREARMS),
    HAMMER_CURL(R.string.ex_hammer_curl, BICEPS, WEIGHT_REPS, FOREARMS),
    PREACHER_CURL(R.string.ex_preacher_curl, BICEPS, WEIGHT_REPS, FOREARMS),
    CABLE_CURL(R.string.ex_cable_curl, BICEPS, WEIGHT_REPS, FOREARMS),

    CLOSE_GRIP_BENCH_PRESS(R.string.ex_close_grip_bench_press, TRICEPS, WEIGHT_REPS, CHEST, SHOULDERS),
    TRICEPS_PUSHDOWN(R.string.ex_triceps_pushdown, TRICEPS, WEIGHT_REPS),
    SKULL_CRUSHER(R.string.ex_skull_crusher, TRICEPS, WEIGHT_REPS),
    OVERHEAD_TRICEPS_EXTENSION(R.string.ex_overhead_triceps_extension, TRICEPS, WEIGHT_REPS),
    DIPS(R.string.ex_dips, TRICEPS, REPS, CHEST, SHOULDERS),

    WRIST_CURL(R.string.ex_wrist_curl, FOREARMS, WEIGHT_REPS),

    SQUAT(R.string.ex_squat, QUADS, WEIGHT_REPS, GLUTES, HAMSTRINGS),
    FRONT_SQUAT(R.string.ex_front_squat, QUADS, WEIGHT_REPS, GLUTES, ABS),
    HACK_SQUAT(R.string.ex_hack_squat, QUADS, WEIGHT_REPS, GLUTES),
    LEG_PRESS(R.string.ex_leg_press, QUADS, WEIGHT_REPS, GLUTES, HAMSTRINGS),
    LEG_EXTENSION(R.string.ex_leg_extension, QUADS, WEIGHT_REPS),
    LUNGE(R.string.ex_lunge, QUADS, WEIGHT_REPS, GLUTES, HAMSTRINGS),
    BULGARIAN_SPLIT_SQUAT(R.string.ex_bulgarian_split_squat, QUADS, WEIGHT_REPS, GLUTES, HAMSTRINGS),

    ROMANIAN_DEADLIFT(R.string.ex_romanian_deadlift, HAMSTRINGS, WEIGHT_REPS, GLUTES, BACK),
    LEG_CURL(R.string.ex_leg_curl, HAMSTRINGS, WEIGHT_REPS, CALVES),
    GOOD_MORNING(R.string.ex_good_morning, HAMSTRINGS, WEIGHT_REPS, GLUTES, BACK),

    HIP_THRUST(R.string.ex_hip_thrust, GLUTES, WEIGHT_REPS, HAMSTRINGS),
    GLUTE_BRIDGE(R.string.ex_glute_bridge, GLUTES, WEIGHT_REPS, HAMSTRINGS),
    CABLE_KICKBACK(R.string.ex_cable_kickback, GLUTES, WEIGHT_REPS, HAMSTRINGS),

    STANDING_CALF_RAISE(R.string.ex_standing_calf_raise, CALVES, WEIGHT_REPS),
    SEATED_CALF_RAISE(R.string.ex_seated_calf_raise, CALVES, WEIGHT_REPS),

    CRUNCH(R.string.ex_crunch, ABS, REPS),
    CABLE_CRUNCH(R.string.ex_cable_crunch, ABS, WEIGHT_REPS),
    HANGING_LEG_RAISE(R.string.ex_hanging_leg_raise, ABS, REPS, FOREARMS),
    RUSSIAN_TWIST(R.string.ex_russian_twist, ABS, REPS),
    AB_WHEEL(R.string.ex_ab_wheel, ABS, REPS, SHOULDERS, BACK),
    PLANK(R.string.ex_plank, ABS, DURATION, SHOULDERS),

    RUNNING(R.string.ex_running, CARDIO, DISTANCE, QUADS, HAMSTRINGS, GLUTES, CALVES),
    CYCLING(R.string.ex_cycling, CARDIO, DISTANCE, QUADS, GLUTES, CALVES),
    ROWING_MACHINE(R.string.ex_rowing_machine, CARDIO, DISTANCE, BACK, QUADS, BICEPS),
    ELLIPTICAL(R.string.ex_elliptical, CARDIO, DURATION, QUADS, GLUTES),
    STAIR_CLIMBER(R.string.ex_stair_climber, CARDIO, DURATION, QUADS, GLUTES, CALVES),
    JUMP_ROPE(R.string.ex_jump_rope, CARDIO, DURATION, CALVES, SHOULDERS);

    val secondary = secondary.toList()

    fun toEntity() = Exercise(builtInKey = name, muscleGroup = muscleGroup, type = type)
}

// Resolved from code rather than the stored column so existing installs pick up changes to built-ins.
val Exercise.secondary: List<MuscleGroup>
    get() = BuiltInExercise.entries.firstOrNull { it.name == builtInKey }?.secondary ?: secondaryMuscles
