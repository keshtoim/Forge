package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.Discipline
import com.keshtoim.forge.data.db.Discipline.CARDIO
import com.keshtoim.forge.data.db.Discipline.POWERLIFTING
import com.keshtoim.forge.data.db.Discipline.STRENGTH
import com.keshtoim.forge.data.db.Discipline.WEIGHTLIFTING
import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Equipment.BARBELL
import com.keshtoim.forge.data.db.Equipment.CABLE
import com.keshtoim.forge.data.db.Equipment.DUMBBELL
import com.keshtoim.forge.data.db.Equipment.MACHINE
import com.keshtoim.forge.data.db.Equipment.NONE
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.Force
import com.keshtoim.forge.data.db.Force.PULL
import com.keshtoim.forge.data.db.Force.PUSH
import com.keshtoim.forge.data.db.Force.STATIC
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.Mechanics.COMPOUND
import com.keshtoim.forge.data.db.Mechanics.ISOLATION
import java.io.Serializable

data class ExerciseTraits(val mechanics: Mechanics?, val discipline: Discipline?, val equipment: Equipment?, val force: Force?)

// Exhaustive on purpose: a new built-in exercise does not compile until its characteristics are set.
val BuiltInExercise.traits: ExerciseTraits
    get() = when (this) {
        BuiltInExercise.BENCH_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.INCLINE_BENCH_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.DECLINE_BENCH_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.DUMBBELL_BENCH_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.INCLINE_DUMBBELL_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.DUMBBELL_FLY -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.CABLE_CROSSOVER -> ExerciseTraits(ISOLATION, STRENGTH, CABLE, PUSH)
        BuiltInExercise.CHEST_PRESS_MACHINE -> ExerciseTraits(COMPOUND, STRENGTH, MACHINE, PUSH)
        BuiltInExercise.PUSH_UP -> ExerciseTraits(COMPOUND, STRENGTH, NONE, PUSH)
        BuiltInExercise.DEADLIFT -> ExerciseTraits(COMPOUND, POWERLIFTING, BARBELL, PULL)
        BuiltInExercise.PULL_UP -> ExerciseTraits(COMPOUND, STRENGTH, NONE, PULL)
        BuiltInExercise.CHIN_UP -> ExerciseTraits(COMPOUND, STRENGTH, NONE, PULL)
        BuiltInExercise.LAT_PULLDOWN -> ExerciseTraits(COMPOUND, STRENGTH, CABLE, PULL)
        BuiltInExercise.BARBELL_ROW -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PULL)
        BuiltInExercise.DUMBBELL_ROW -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PULL)
        BuiltInExercise.SEATED_CABLE_ROW -> ExerciseTraits(COMPOUND, STRENGTH, CABLE, PULL)
        BuiltInExercise.T_BAR_ROW -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PULL)
        BuiltInExercise.HYPEREXTENSION -> ExerciseTraits(ISOLATION, STRENGTH, MACHINE, PULL)
        BuiltInExercise.SHRUG -> ExerciseTraits(ISOLATION, STRENGTH, BARBELL, PULL)
        BuiltInExercise.OVERHEAD_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.DUMBBELL_SHOULDER_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.ARNOLD_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.LATERAL_RAISE -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.FRONT_RAISE -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.REAR_DELT_FLY -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PULL)
        BuiltInExercise.FACE_PULL -> ExerciseTraits(ISOLATION, STRENGTH, CABLE, PULL)
        BuiltInExercise.UPRIGHT_ROW -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PULL)
        BuiltInExercise.BARBELL_CURL -> ExerciseTraits(ISOLATION, STRENGTH, BARBELL, PULL)
        BuiltInExercise.DUMBBELL_CURL -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PULL)
        BuiltInExercise.HAMMER_CURL -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PULL)
        BuiltInExercise.PREACHER_CURL -> ExerciseTraits(ISOLATION, STRENGTH, BARBELL, PULL)
        BuiltInExercise.CABLE_CURL -> ExerciseTraits(ISOLATION, STRENGTH, CABLE, PULL)
        BuiltInExercise.CLOSE_GRIP_BENCH_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.TRICEPS_PUSHDOWN -> ExerciseTraits(ISOLATION, STRENGTH, CABLE, PUSH)
        BuiltInExercise.SKULL_CRUSHER -> ExerciseTraits(ISOLATION, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.OVERHEAD_TRICEPS_EXTENSION -> ExerciseTraits(ISOLATION, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.DIPS -> ExerciseTraits(COMPOUND, STRENGTH, NONE, PUSH)
        BuiltInExercise.WRIST_CURL -> ExerciseTraits(ISOLATION, STRENGTH, BARBELL, PULL)
        BuiltInExercise.SQUAT -> ExerciseTraits(COMPOUND, POWERLIFTING, BARBELL, PUSH)
        BuiltInExercise.FRONT_SQUAT -> ExerciseTraits(COMPOUND, WEIGHTLIFTING, BARBELL, PUSH)
        BuiltInExercise.HACK_SQUAT -> ExerciseTraits(COMPOUND, STRENGTH, MACHINE, PUSH)
        BuiltInExercise.LEG_PRESS -> ExerciseTraits(COMPOUND, STRENGTH, MACHINE, PUSH)
        BuiltInExercise.LEG_EXTENSION -> ExerciseTraits(ISOLATION, STRENGTH, MACHINE, PUSH)
        BuiltInExercise.LUNGE -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.BULGARIAN_SPLIT_SQUAT -> ExerciseTraits(COMPOUND, STRENGTH, DUMBBELL, PUSH)
        BuiltInExercise.ROMANIAN_DEADLIFT -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PULL)
        BuiltInExercise.LEG_CURL -> ExerciseTraits(ISOLATION, STRENGTH, MACHINE, PULL)
        BuiltInExercise.GOOD_MORNING -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PULL)
        BuiltInExercise.HIP_THRUST -> ExerciseTraits(COMPOUND, STRENGTH, BARBELL, PUSH)
        BuiltInExercise.GLUTE_BRIDGE -> ExerciseTraits(ISOLATION, STRENGTH, NONE, PUSH)
        BuiltInExercise.CABLE_KICKBACK -> ExerciseTraits(ISOLATION, STRENGTH, CABLE, PUSH)
        BuiltInExercise.STANDING_CALF_RAISE -> ExerciseTraits(ISOLATION, STRENGTH, MACHINE, PUSH)
        BuiltInExercise.SEATED_CALF_RAISE -> ExerciseTraits(ISOLATION, STRENGTH, MACHINE, PUSH)
        BuiltInExercise.CRUNCH -> ExerciseTraits(ISOLATION, STRENGTH, NONE, PULL)
        BuiltInExercise.CABLE_CRUNCH -> ExerciseTraits(ISOLATION, STRENGTH, CABLE, PULL)
        BuiltInExercise.HANGING_LEG_RAISE -> ExerciseTraits(ISOLATION, STRENGTH, NONE, PULL)
        BuiltInExercise.RUSSIAN_TWIST -> ExerciseTraits(ISOLATION, STRENGTH, NONE, Force.OTHER)
        BuiltInExercise.AB_WHEEL -> ExerciseTraits(COMPOUND, STRENGTH, Equipment.OTHER, Force.OTHER)
        BuiltInExercise.PLANK -> ExerciseTraits(Mechanics.OTHER, STRENGTH, NONE, STATIC)
        BuiltInExercise.RUNNING -> ExerciseTraits(Mechanics.OTHER, CARDIO, NONE, Force.OTHER)
        BuiltInExercise.CYCLING -> ExerciseTraits(Mechanics.OTHER, CARDIO, MACHINE, Force.OTHER)
        BuiltInExercise.ROWING_MACHINE -> ExerciseTraits(Mechanics.OTHER, CARDIO, MACHINE, PULL)
        BuiltInExercise.ELLIPTICAL -> ExerciseTraits(Mechanics.OTHER, CARDIO, MACHINE, Force.OTHER)
        BuiltInExercise.STAIR_CLIMBER -> ExerciseTraits(Mechanics.OTHER, CARDIO, MACHINE, Force.OTHER)
        BuiltInExercise.JUMP_ROPE -> ExerciseTraits(Mechanics.OTHER, CARDIO, Equipment.OTHER, Force.OTHER)
    }

// Built-ins come from code so existing installs pick up corrections; custom exercises use their own columns.
val Exercise.traits: ExerciseTraits
    get() = builtIn?.traits ?: ExerciseTraits(mechanics, discipline, equipment, force)

// Within a characteristic any picked value matches; across characteristics all must match. Nothing picked lets everything through.
data class TraitFilter(
    val mechanics: Set<Mechanics> = emptySet(),
    val discipline: Set<Discipline> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
    val force: Set<Force> = emptySet(),
) : Serializable {
    val size get() = mechanics.size + discipline.size + equipment.size + force.size

    fun matches(traits: ExerciseTraits) =
        traits.mechanics.passes(mechanics) && traits.discipline.passes(discipline) && traits.equipment.passes(equipment) && traits.force.passes(force)

    private fun <T> T?.passes(picked: Set<T>) = picked.isEmpty() || this in picked
}
