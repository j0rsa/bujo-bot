package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.ActionId
import com.j0rsa.bujo.telegram.BujoMarkup.valueMarkup
import com.j0rsa.bujo.telegram.BujoMarkup.valueTypeMarkup
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.actor.common.optionalStep
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.monad.ActorContext

data class AddValueState(
	override val ctx: ActorContext,
	val actionId: ActionId,
	var type: ValueType = ValueType.values().random(),
	var name: String = "",
	var value: String = ""
) : ActorState(ctx)

object AddValueActor : StateMachineActor<AddValueState>(
	mandatoryStep({
		sendLocalizedMessage(state, Lines::addActionValueInitMessage, valueTypeMarkup())
	}, {
		try {
			state.type = ValueType.valueOf(message.text)
		} catch (e: Exception) {
			return@mandatoryStep false
		}
		state.name = state.type.name
		true
	}),
	optionalStep({
		sendLocalizedMessage(state, listOf(Lines::addActionValueNameMessage, Lines::orTapSkipMessage))
	}, {
		state.name = message.text
		true
	}),
	mandatoryStep({
		sendLocalizedMessage(state, Lines::addActionValueValueMessage, valueMarkup(state.type))
	}, {
		state.value = message.text
		with(state) {
			ctx.client.addValue(user.id, actionId, Value(type, value, name)).fold(
				{
					!sendLocalizedMessage(state, Lines::addActionValueNotRegistered)
				},
				{
					sendLocalizedMessage(state, Lines::addActionValueRegistered)
				}
			)
		}
	})
)
