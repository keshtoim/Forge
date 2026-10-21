package com.keshtoim.forge.ui.templates

import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.data.runs
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateItemsTest {
    private fun list(vararg supersets: Int?) =
        supersets.mapIndexed { i, s -> TemplateExercise(templateId = 0, exerciseId = i + 1L, position = i, targetSets = i + 2, superset = s) }.toMutableList()

    private val MutableList<TemplateExercise>.ids get() = map { it.exerciseId }
    private val MutableList<TemplateExercise>.groups get() = map { it.superset }

    @Test
    fun `linking joins neighbours and shares the upper exercise's rounds`() {
        val items = list(null, null, null)
        items.linkWithNext(0)
        assertEquals(listOf(1, 1, null), items.groups)
        assertEquals(listOf(2, 2, 4), items.map { it.targetSets })

        items.linkWithNext(1)
        assertEquals(listOf(1, 1, 1), items.groups)
        assertEquals(listOf(listOf(0, 1, 2)), items.runs().map { it.toList() })
    }

    @Test
    fun `sets of a superset member change the whole superset`() {
        val items = list(null, 1, 1)
        items.setSets(2, 5)
        assertEquals(listOf(2, 5, 5), items.map { it.targetSets })
    }

    @Test
    fun `a single exercise jumps over a superset instead of splitting it`() {
        val items = list(null, 1, 1)
        items.moveItem(0, 1)
        assertEquals(listOf(2L, 3L, 1L), items.ids)
        assertEquals(listOf(1, 1, null), items.groups)
    }

    @Test
    fun `moving a member out of a pair or removing one dissolves the superset`() {
        val moved = list(1, 1, null)
        moved.moveItem(1, 1)
        assertEquals(listOf(1L, 3L, 2L), moved.ids)
        assertEquals(listOf(null, null, null), moved.groups)

        val removed = list(1, 1, 2, 2)
        removed.removeItem(0)
        assertEquals(listOf(null, 1, 1), removed.groups)
    }

    @Test
    fun `members reorder inside their superset`() {
        val items = list(1, 1, 1)
        items.moveItem(2, -1)
        assertEquals(listOf(1L, 3L, 2L), items.ids)
        assertEquals(listOf(1, 1, 1), items.groups)
    }
}
