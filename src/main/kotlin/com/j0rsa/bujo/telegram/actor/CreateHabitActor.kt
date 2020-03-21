package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.BujoLogic
import com.j0rsa.bujo.telegram.BujoMarkup.periodMarkup
import com.j0rsa.bujo.telegram.BujoMarkup.valueTypeMarkup
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.Period
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.api.model.ValueTemplate
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.channels.SendChannel
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
	val values: MutableList<ValueTemplate> = mutableListOf(),
	var valuesActor: SendChannel<ActorMessage>? = null
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
	optionalStep({
		sendLocalizedMessage(
			state,
			listOf(Lines::doYouWantToAddValueMessage, Lines::orTapSkipMessage),
			valueTypeMarkup(state.user.language)
		)
	}, {
		if (state.valuesActor == null) {
			val values = state.values
			state.valuesActor = ValueTemplateActor().yield(ValueTemplateState(state.ctx)) {
				values.add(
					ValueTemplate(state.type ?: return@`yield`, state.name)
				)
			}
		} else {
			BujoLogic.handleSayActorMessage(message.text, state.valuesActor!!)
		}
		true
	})
)
