package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetAdviceTest {
    @Test
    fun `advice is read from a fenced reply and fills only what it suggests`() {
        val reply = "Вот совет:\n```json\n{\"weightKg\": 62.5, \"reps\": 6, \"durationSec\": null, \"distanceM\": null, \"reason\": \"Прошлый подход был тяжёлым\"}\n```"
        val advice = parseSetAdvice(reply)!!
        assertEquals(SetAdvice(weightKg = 62.5, reps = 6, reason = "Прошлый подход был тяжёлым"), advice)

        val set = WorkoutSet(workoutExerciseId = 1, position = 0, weightKg = 60.0, reps = 8, durationSec = 30)
        assertEquals(set.copy(weightKg = 62.5, reps = 6), advice.applyTo(set))
    }

    @Test
    fun `replies without a usable suggestion are dropped`() {
        assertNull(parseSetAdvice("Сделай ещё подход с тем же весом"))
        assertNull(parseSetAdvice("""{"reason": "Отдохни"}"""))
        assertNull(parseSetAdvice("""{"weightKg": -5, "reps": 6}"""))
        assertNull(parseSetAdvice("""{"weightKg": 60, "reps": 0}"""))
    }
}
