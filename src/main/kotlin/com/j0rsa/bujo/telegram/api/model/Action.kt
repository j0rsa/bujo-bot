package com.j0rsa.bujo.telegram.api.model

import java.util.*

/**
 * @author red
 * @since 08.02.20
 */

data class ActionRequest(
	val description: String,
	val tags: List<TagRequest>
) {
	constructor(description: String, text: String) : this(description, text.splitToTags())

	companion object {
		private fun String.splitToTags() = this.split(",").map { TagRequest.fromString(it) }
	}
}

data class Action(
	val description: String,
	val tags: List<Tag>,
	val habitId: HabitId? = null,
	val id: ActionId? = null
)

inline class ActionId(val value: UUID) {
	companion object {
		fun randomValue() = ActionId(UUID.randomUUID())
	}
}