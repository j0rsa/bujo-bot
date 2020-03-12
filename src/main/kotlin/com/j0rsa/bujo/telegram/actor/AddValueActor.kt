package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.valueMarkup
import com.j0rsa.bujo.telegram.valueTypeMarkup

data class AddValueState(
	override val ctx: ActorContext,
	val actionId: ActionId,
	var type: ValueType = ValueType.values().random(),
	var name: String = "",
	var value: String = ""
) : ActorState(ctx)

object AddValueActor : StateMachineActor<AddValueState>(
	initStep {
		sendLocalizedMessage(state, Lines::addActionValueInitMessage, valueTypeMarkup())
	},
	mandatoryStep {
		try {
			state.type = ValueType.valueOf(message.text)
		} catch (e: Exception) {
			return@mandatoryStep false
		}
		state.name = state.type.name
		sendLocalizedMessage(state, listOf(Lines::addActionValueNameMessage, Lines::orTapSkipMessage))
	},
	optionalStep({
		state.name = message.text
		true
	}, {
		sendLocalizedMessage(this, Lines::addActionValueValueMessage, valueMarkup(type))
	}),
	mandatoryStep {
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
	}
)
