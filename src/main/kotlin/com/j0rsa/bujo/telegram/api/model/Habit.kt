package com.j0rsa.bujo.telegram.api.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Period
import java.util.*

/**
 * @author red
 * @since 08.02.20
 */

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

inline class HabitId(val value: UUID) {
    companion object {
        fun randomValue() = HabitId(UUID.randomUUID())
    }
}