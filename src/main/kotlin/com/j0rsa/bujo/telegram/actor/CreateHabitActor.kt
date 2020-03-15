package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.BujoMarkup.habitCreatedMarkup
import com.j0rsa.bujo.telegram.BujoMarkup.periodMarkup
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.HabitRequest
import com.j0rsa.bujo.telegram.api.model.Period
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.api.model.ValueTemplate
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
	var period: Period = Period.Day,
	var quote: String? = null,
	var bad: Boolean? = null,
	var startFrom: LocalDateTime? = null,
	var values: List<ValueTemplate> = emptyList()
) : ActorState(ctx)

object HabitActor : StateMachineActor<CreateHabitState>(
	mandatoryStep({
		sendLocalizedMessage(state, Lines::createHabitInitMessage)
	}, {
		state.name = message.text
		true
	}),
	mandatoryStep({
		sendLocalizedMessage(state, Lines::createHabitTagsMessage)
	}, {
		state.tags = message.text.split(",").map { TagRequest.fromString(it) }
		true
	}),
	mandatoryStep({
		sendLocalizedMessage(state, Lines::createHabitPeriodMessage, periodMarkup(state.user.language))
	}, {
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
				true
			}
		}
	}),
	mandatoryStep({
		sendLocalizedMessage(state, Lines::createHabitNumberOfRepetitionsMessage)
	}, {
		try {
			state.numberOfRepetitions = Integer.parseUnsignedInt(message.text)
			true
		} catch (e: NumberFormatException) {
			!sendLocalizedMessage(
				state,
				listOf(
					Lines::badInputMessage,
					Lines::createHabitNumberOfRepetitionsMessage
				)
			)
		}
	}),
	optionalStep({
		sendLocalizedMessage(state, listOf(Lines::createHabitQuoteMessage, Lines::orTapSkipMessage))
	}, {
		state.quote = message.text.trim()
		true
	}),
	executionStep {
		val habitRequest = HabitRequest(name, tags, numberOfRepetitions, period, quote, bad, startFrom)
		ctx.client.createHabit(user.id, habitRequest).fold(
			{
				!sendLocalizedMessage(this, Lines::habitNotRegisteredMessage)
			},
			{ habitId ->
				sendLocalizedMessage(
					this,
					Lines::habitRegisteredMessage,
					habitCreatedMarkup(user.language, habitId)
				)
			})
	}
)
