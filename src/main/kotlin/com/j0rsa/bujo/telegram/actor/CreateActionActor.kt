package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.monad.ActorContext

/**
 * @author red
 * @since 09.02.20
 */
data class CreateActionState(
	override val ctx: ActorContext,
	var actionDescription: String = "",
	var tags: List<TagRequest> = emptyList()
) : ActorState(ctx)

object CreateActionActor : StateMachineActor<CreateActionState>(
	mandatoryStep(
		{
			sendLocalizedMessage(state, Lines::actionCreationInitMessage)
		}, {
			state.actionDescription = message.text
			true
		}),
	mandatoryStep({
		sendLocalizedMessage(state, Lines::actionCreationTagsInput)
	}, {
		state.tags = message.text.split(",").map { TagRequest.fromString(it) }
		true
	})
)