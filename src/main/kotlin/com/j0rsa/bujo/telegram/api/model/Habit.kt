package com.j0rsa.bujo.telegram.api.model

import com.j0rsa.bujo.telegram.bot.HabitId
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZonedDateTime

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
    val name: String = "",
    val tags: List<Tag> = emptyList(),
    val numberOfRepetitions: Int = 0,
    val period: Period = Period.Day,
    val quote: String? = null,
    val bad: Boolean? = false,
    val startFrom: ZonedDateTime? = null,
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
    val name: String? = null,
    val values: List<String> = emptyList()
)