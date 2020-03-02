package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.valueMarkup
import com.j0rsa.bujo.telegram.valueTypeMarkup
import kotlinx.coroutines.ObsoleteCoroutinesApi

data class AddValueState(
	override val ctx: ActorContext,
	val actionId: ActionId,
	var type: ValueType = ValueType.values().random(),
	var name: String = "",
	var value: String = ""
) : ActorState(ctx)

@ObsoleteCoroutinesApi
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
		sendLocalizedMessage(state, Lines::addActionValueNameMessage)
	},
	optionalStep {
		//execute
		state.name = message.text
		//init
		sendLocalizedMessage(state, Lines::addActionValueValueMessage, valueMarkup(state.type))
	},
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
