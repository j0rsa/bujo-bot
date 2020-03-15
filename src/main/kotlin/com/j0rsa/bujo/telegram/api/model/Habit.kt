package com.j0rsa.bujo.telegram.api.model

import com.j0rsa.bujo.telegram.HabitId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * @author red
 * @since 08.02.20
 */

data class HabitRequest(
    val name: String,
    val tags: List<TagRequest>,
    val numberOfRepetitions: Int,
    val period: Period,
    val quote: String?,
    val bad: Boolean?,
    val startFrom: LocalDateTime?,
    val values: List<ValueTemplate> = emptyList()
)

data class Habit(
    val name: String,
    val tags: List<Tag>,
    val numberOfRepetitions: Int,
    val period: Period,
    val quote: String?,
    val bad: Boolean?,
    val startFrom: LocalDateTime?,
    val id: HabitId? = null
)

enum class Period {
    Week, Day
}

data class HabitsInfo(
    val habit: Habit,
    val currentStreak: BigDecimal = BigDecimal.ZERO
)

data class HabitInfoView(
    val habit: Habit,
    val streakRow: StreakRow
)

data class StreakRow(
    val currentStreak: BigDecimal = BigDecimal.ZERO,
    val maxStreak: BigDecimal = BigDecimal.ZERO
)

data class ValueTemplate(
    val type: ValueType,
    val values: List<String> = emptyList(),
    val name: String? = null
)