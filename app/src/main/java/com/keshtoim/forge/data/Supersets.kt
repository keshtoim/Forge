package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.TemplateExercise

// Index ranges of consecutive items that go together: a superset, or a single exercise outside one.
fun List<TemplateExercise>.runs(): List<IntRange> {
    val result = mutableListOf<IntRange>()
    var start = 0
    for (i in 1..size) {
        if (i == size || this[i].superset == null || this[i].superset != this[start].superset) {
            result += start until i
            start = i
        }
    }
    return result
}

// Renumbers groups in list order and dissolves ones left with a single exercise.
fun MutableList<TemplateExercise>.normalizeSupersets() {
    var next = 1
    runs().forEach { run ->
        val group = if (run.count() > 1) next++ else null
        run.forEach { if (this[it].superset != group) this[it] = this[it].copy(superset = group) }
    }
}
