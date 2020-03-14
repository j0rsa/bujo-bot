package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.BujoMarkup.habitCreatedMarkup
import com.j0rsa.bujo.telegram.BujoMarkup.periodMarkup
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.HabitRequest
import com.j0rsa.bujo.telegram.api.model.Period
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.monad.ActorContext
import java.time.LocalDateTime

/**
 * @author red
 * @since 09.02.20
 */

data class CreateHabitState(
	override val ctx: ActorContext,
	var name: String = "",
	var tags: List<TagRequest> = emptyList(),
	var numberOfRepetitions: Int = 0,
	var period: Period = Period.DAILY,
	var quote: String? = null,
	var bad: Boolean? = null,
	var startFrom: LocalDateTime? = null
) : ActorState(ctx)

object HabitActor : StateMachineActor<CreateHabitState>(
	initStep {
		sendLocalizedMessage(state, Lines::createHabitInitMessage)
	},
	mandatoryStep {
		state.name = message.text
		sendLocalizedMessage(state, Lines::createHabitTagsMessage)
	},
	mandatoryStep {
		state.tags = message.text.split(",").map { TagRequest.fromString(it) }
		sendLocalizedMessage(state, Lines::createHabitPeriodMessage, periodMarkup(state.user.language))
	},
	mandatoryStep {
		when (val period = Period.values().find { it.name == message.text }) {
			null -> {
				!sendLocalizedMessage(
					state,
					listOf(
						Lines::badInputMessage,
						Lines::createHabitPeriodMessage
					),
					periodMarkup(state.user.language)
				)
			}
			else -> {
				state.period = period
				sendLocalizedMessage(state, Lines::createHabitNumberOfRepetitionsMessage)
			}
		}
	},
	mandatoryStep {
		try {
			state.numberOfRepetitions = Integer.parseUnsignedInt(message.text)
			sendLocalizedMessage(state, listOf(Lines::createHabitQuoteMessage, Lines::orTapSkipMessage))
		} catch (e: NumberFormatException) {
			!sendLocalizedMessage(
				state,
				listOf(
					Lines::badInputMessage,
					Lines::createHabitNumberOfRepetitionsMessage
				)
			)
		}
	},
	optionalStep({
		state.quote = message.text.trim()
		true
	}, {
		ctx.client.createHabit(
			user.id, HabitRequest(name, tags, numberOfRepetitions, period, quote, bad, startFrom)
		).fold(
			{
				!sendLocalizedMessage(this, Lines::actionNotRegisteredMessage)
			},
			{ habitId ->
				sendLocalizedMessage(
					this,
					Lines::actionRegisteredMessage,
					habitCreatedMarkup(user.language, habitId)
				)
			})
	})
)
