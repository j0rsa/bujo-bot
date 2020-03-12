package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.initStep
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.createdAction
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
	initStep {
		sendLocalizedMessage(state, Lines::actionCreationInitMessage)
	},
	mandatoryStep {
		state.actionDescription = message.text
		sendLocalizedMessage(state, Lines::actionCreationTagsInput)
	},
	mandatoryStep {
		state.tags = message.text.split(",").map { TagRequest.fromString(it) }
		with(state) {
			ctx.client.createAction(user.id, ActionRequest(actionDescription, tags)).fold(
				{
					!sendLocalizedMessage(state, Lines::actionNotRegisteredMessage)
				},
				{ actionId ->
					sendLocalizedMessage(state, Lines::actionRegisteredMessage, createdAction(actionId))
				})
		}
	}
)
