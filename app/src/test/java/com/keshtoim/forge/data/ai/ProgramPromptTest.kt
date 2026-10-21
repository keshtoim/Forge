package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramPromptTest {
    @Test
    fun `the prompt lists the catalog by id and the request`() {
        val prompt = programPrompt(
            "Russian",
            ProgramRequest(ProgramGoal.FAT_LOSS, 3, 60, ProgramLevel.BEGINNER, setOf(Equipment.DUMBBELL), "болит колено"),
            Athlete(),
            listOf(CatalogEntry(12, "Жим гантелей лёжа", MuscleGroup.CHEST, Equipment.DUMBBELL, Mechanics.COMPOUND, ExerciseType.WEIGHT_REPS)),
        )
        assertTrue(prompt.contains("Goal: fat loss. Level: beginner."))
        assertTrue(prompt.contains("Workouts per week: 3, about 60 minutes"))
        assertTrue(prompt.contains("wishes and limits: болит колено"))
        assertTrue(prompt.contains("12 | Жим гантелей лёжа | chest | dumbbell | compound | weight_reps"))
        assertTrue(prompt.contains("exactly 3 workouts"))
    }

    @Test
    fun `a reply becomes templates from known exercises with tidy supersets`() {
        val reply = """Вот программа:
            ```json
            {"name":"Сила на 3 дня","notes":"Прибавляй вес, когда все подходы даются легко.","workouts":[
              {"name":"День A","exercises":[{"id":1,"sets":4},{"id":2,"sets":3,"superset":7},{"id":3,"sets":5,"superset":7},{"id":99,"sets":3}]},
              {"name":"День B","exercises":[{"id":4,"sets":3,"superset":2}]},
              {"name":"","exercises":[{"id":42,"sets":3}]}
            ]}
            ```"""
        val draft = parseProgram(reply, known = setOf(1L, 2L, 3L, 4L))!!

        assertEquals("Сила на 3 дня", draft.name)
        assertEquals(listOf("День A", "День B"), draft.workouts.map { it.name })
        val dayA = draft.workouts.first().items
        assertEquals(listOf(1L, 2L, 3L), dayA.map { it.exerciseId })
        assertEquals(listOf(null, 1, 1), dayA.map { it.superset })
        assertEquals(listOf(4, 3, 3), dayA.map { it.targetSets })
        // A superset of one is just an exercise.
        assertEquals(null, draft.workouts[1].items.single().superset)
    }

    @Test
    fun `a reply without usable workouts is rejected`() {
        assertNull(parseProgram("Не могу составить программу", setOf(1L)))
        assertNull(parseProgram("""{"name":"X","workouts":[{"name":"A","exercises":[{"id":5,"sets":3}]}]}""", setOf(1L)))
    }
}
