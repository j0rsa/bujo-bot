package com.j0rsa.bujo.telegram.api.model

import java.util.*

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

inline class ActionId(val value: UUID) {
	companion object {
		fun randomValue() = ActionId(UUID.randomUUID())
		fun fromString(s: String) = ActionId(UUID.fromString(s))
	}
}

inline class ValueId(val value: UUID) {
	companion object {
		fun randomValue() = ValueId(UUID.randomUUID())
	}
}

data class Value(
	val type: ValueType,
	val value: String?,
	val name: String?
)

enum class ValueType {
	Mood,
	EndDate
}