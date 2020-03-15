package com.j0rsa.bujo.telegram.api.model

import com.j0rsa.bujo.telegram.ActionId
import com.j0rsa.bujo.telegram.HabitId
import com.j0rsa.bujo.telegram.Lines
import kotlin.reflect.KProperty1

/**
 * @author red
 * @since 08.02.20
 */

data class ActionRequest(
	val description: String,
	val tags: List<TagRequest>
)

data class Action(
	val description: String,
	val tags: List<Tag>,
	val habitId: HabitId? = null,
	val id: ActionId? = null,
	val values: List<Value> = emptyList()
)

data class Value(
	val type: ValueType,
	val value: String?,
	val name: String?
)

enum class ValueType(val caption: KProperty1<Lines, String>) {
	Mood(Lines::moodButton),
	EndDate(Lines::endDateButton);
}