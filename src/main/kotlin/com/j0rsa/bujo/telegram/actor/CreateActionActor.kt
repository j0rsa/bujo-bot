package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.monad.ActorContext

/**
 * @author red
 * @since 09.02.20
 */
data class CreateActionState(
	override val ctx: ActorContext,
	override val trackerUser: TrackerUser,
	var actionDescription: String = "",
	var tags: List<TagRequest> = emptyList()
) : ActorState(ctx, trackerUser)

object CreateActionActor : StateMachineActor<CreateActionState, ActionRequest>(
	{
		ActionRequest(actionDescription, tags)
	},
	mandatoryStep(
		{
			sendLocalizedMessage(listOf(Lines::actionCreationInitMessage, Lines::actionCreationDescriptionInput))
		}, {
			state.actionDescription = message.text
			true
		}),
	mandatoryStep({
		sendLocalizedMessage(Lines::actionCreationTagsInput)
	}, {
		state.tags = message.text.split(",").map { TagRequest.fromString(it) }
		true
	})
)