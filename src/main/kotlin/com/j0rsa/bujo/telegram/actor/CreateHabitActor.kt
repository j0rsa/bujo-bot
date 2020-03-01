package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.ObsoleteCoroutinesApi

/**
 * @author red
 * @since 09.02.20
 */

data class CreateHabitState(
	override val ctx: ActorContext,
	var name: String = "",
	var duration: String = ""
) : ActorState(ctx)

@ObsoleteCoroutinesApi
object HabitActor : StateMachineActor<CreateHabitState>(
	initStep {
		sendLocalizedMessage(state, Lines::createHabitInitMessage)
	},
	mandatoryStep {
		state.name = message.text
		sendLocalizedMessage(state, Lines::createHabitDurationMessage)
	},
	mandatoryStep {
		state.duration = message.text
		true
	}
)
