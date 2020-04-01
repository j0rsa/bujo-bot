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
	internal var name: String = "",
	internal var tags: List<TagRequest> = emptyList(),
	internal var numberOfRepetitions: Int = 0,
	internal var period: Period = Period.Day,
	internal var quote: String? = null,
	internal var bad: Boolean? = null,
	internal var startFrom: LocalDateTime? = null,
	internal val values: MutableList<ValueTemplate> = mutableListOf()
) : ActorState(ctx, trackerUser)

object HabitActor : StateMachineActor<CreateHabitState, HabitRequest>(
	{
		HabitRequest(name, tags, numberOfRepetitions, period, quote, bad, startFrom, values)
	},
	mandatoryStep({
		sendLocalizedMessage(Lines::createHabitInitMessage)
	}, {
		state.name = message.text
		true
	}),
	mandatoryStep({
		sendLocalizedMessage(Lines::createHabitTagsMessage)
	}, {
		state.tags = message.text.split(",").map { TagRequest.fromString(it) }
		true
	}),
	mandatoryStep({
		sendLocalizedMessage(Lines::createHabitPeriodMessage, periodMarkup(state.trackerUser.language))
	}, {
		when (val period = Period.values().find { it.name == message.text }) {
			null -> {
				!sendLocalizedMessage(
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
		sendLocalizedMessage(Lines::createHabitNumberOfRepetitionsMessage)
	}, {
		try {
			state.numberOfRepetitions = Integer.parseUnsignedInt(message.text)
			true
		} catch (e: NumberFormatException) {
			!sendLocalizedMessage(
				listOf(
					Lines::badInputMessage,
					Lines::createHabitNumberOfRepetitionsMessage
				)
			)
		}
	}),
	optionalStep({
		sendLocalizedMessage(listOf(Lines::createHabitQuoteMessage, Lines::orTapSkipMessage))
	}, {
		state.quote = message.text.trim()
		true
	}),
	optionalStep({
		sendLocalizedMessage(
			listOf(Lines::doYouWantToAddValueMessage, Lines::orTapSkipMessage),
			valueTypeMarkup(state.trackerUser.language)
		)
	}, {
		if (state.subActor == DummyChannel) {
			val superState = state
			state.subActor = ValueTemplateActor
				.yield(ValueTemplateState(state.ctx, state.trackerUser, ValueType.valueOf(message.text))) {
					superState.values.add(result)
					superState.subActor = DummyChannel
					sendLocalizedMessage(
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
