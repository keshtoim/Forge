package com.keshtoim.forge.ui.progress

import com.keshtoim.forge.data.db.SetPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricTest {
    private fun set(workoutId: Long = 1, startedAt: Long = 0, weight: Double? = null, reps: Int? = null, durationSec: Int? = null, distanceM: Double? = null) =
        SetPoint(workoutId, startedAt, weight, reps, durationSec, distanceM)

    @Test
    fun `e1rm uses epley and treats single rep as the weight itself`() {
        assertEquals(100.0, Metric.E1RM.of(listOf(set(weight = 100.0, reps = 1)))!!, 0.001)
        assertEquals(133.333, Metric.E1RM.of(listOf(set(weight = 100.0, reps = 10)))!!, 0.001)
    }

    @Test
    fun `e1rm takes the best set and ignores incomplete ones`() {
        val sets = listOf(
            set(weight = 100.0, reps = 5),
            set(weight = 90.0, reps = 10),
            set(weight = 200.0, reps = null),
            set(weight = null, reps = 20),
        )
        assertEquals(120.0, Metric.E1RM.of(sets)!!, 0.001)
    }

    @Test
    fun `volume sums weight times reps`() {
        val sets = listOf(set(weight = 100.0, reps = 5), set(weight = 80.0, reps = 10), set(reps = 10))
        assertEquals(1300.0, Metric.VOLUME.of(sets)!!, 0.0)
    }

    @Test
    fun `metrics without data return null`() {
        assertNull(Metric.VOLUME.of(listOf(set(reps = 10))))
        assertNull(Metric.MAX_WEIGHT.of(emptyList()))
        assertNull(Metric.TOTAL_REPS.of(listOf(set(weight = 50.0))))
    }

    @Test
    fun `duration and distance are converted to minutes and km`() {
        assertEquals(1.5, Metric.MAX_DURATION.of(listOf(set(durationSec = 90), set(durationSec = 60)))!!, 0.0)
        assertEquals(5.0, Metric.MAX_DISTANCE.of(listOf(set(distanceM = 5000.0)))!!, 0.0)
    }

    @Test
    fun `series has one point per workout ordered by time`() {
        val sets = listOf(
            set(workoutId = 2, startedAt = 200, weight = 70.0, reps = 5),
            set(workoutId = 1, startedAt = 100, weight = 60.0, reps = 5),
            set(workoutId = 2, startedAt = 200, weight = 75.0, reps = 3),
            set(workoutId = 3, startedAt = 300, reps = 5),
        )
        val series = Metric.MAX_WEIGHT.series(sets)
        assertEquals(listOf(Point(100, 60.0), Point(200, 75.0)), series)
    }
}
