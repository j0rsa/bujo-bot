package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.bot.BujoLogic
import com.j0rsa.bujo.telegram.bot.Markup.periodMarkup
import com.j0rsa.bujo.telegram.bot.Markup.valueTypeMarkup
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import java.time.LocalDateTime

/**
 * @author red
 * @since 09.02.20
 */

data class CreateHabitState(
	override val ctx: ActorContext,
	override val trackerUser: TrackerUser,
	var name: String = "",
	var tags: List<TagRequest> = emptyList(),
	var numberOfRepetitions: Int = 0,
	var period: Period = Period.Day,
	var quote: String? = null,
	var bad: Boolean? = null,
	var startFrom: LocalDateTime? = null,
	val values: MutableList<ValueTemplate> = mutableListOf()
) : ActorState(ctx, trackerUser)

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
		sendLocalizedMessage(state, Lines::createHabitPeriodMessage, periodMarkup(state.trackerUser.language))
	}, {
		when (val period = Period.values().find { it.name == message.text }) {
			null -> {
				!sendLocalizedMessage(
					state,
					listOf(
						Lines::badInputMessage,
						Lines::createHabitPeriodMessage
					),
					periodMarkup(state.trackerUser.language)
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
			valueTypeMarkup(state.trackerUser.language)
		)
	}, {
		if (state.subActor == DummyChannel) {
			val superState = state
			state.subActor = ValueTemplateActor
				.yield(ValueTemplateState(state.ctx, state.trackerUser, ValueType.valueOf(message.text))) {
					superState.values.add(
						ValueTemplate(state.type, state.name)
					)
					superState.subActor = DummyChannel
					sendLocalizedMessage(
						state,
						listOf(
							Lines::valueAddedMessage,
							Lines::doYouWantToAddValueMessage,
							Lines::orTapSkipMessage
						),
						valueTypeMarkup(state.trackerUser.language)
					)
				}
		} else {
			BujoLogic.handleSayActorMessage(message.text, state.subActor)
		}
		false
	})
)
