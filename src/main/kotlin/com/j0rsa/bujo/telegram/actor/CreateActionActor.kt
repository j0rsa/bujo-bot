package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import arrow.core.Either.Right
import com.j0rsa.bujo.telegram.BotError
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Reader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

/**
 * @author red
 * @since 09.02.20
 */
const val INIT_ACTION_TEXT = """You are creating an action\n
                    Enter action description"""
const val TAGS = "Enter actions tags (comma separated)"
const val ACTION_SUCCESS = "Action was registered"
const val ACTION_FAILED = "Action was not registered \uD83D\uDE22"

object CreateActionActor : Actor {
	@UseExperimental(ObsoleteCoroutinesApi::class)
	override fun yield(
		chatId: Long,
		userId: Long
	): Reader<ActorContext, SendChannel<ActorMessage>> =
		Reader.ask<ActorContext>().map { ctx -> yield(chatId, userId, ctx) }

	@UseExperimental(ObsoleteCoroutinesApi::class)
	private fun yield(chatId: Long, userId: Long, ctx: ActorContext) = with(ctx.scope) {
		actor<ActorMessage> {
			val user = ctx.client.getUser(userId)
			var actionDescription = ""
			val sendMessage = { text: String -> ctx.bot.sendMessage(chatId, text) }
			val createAction = { text: String ->
				ctx.client.createAction(user.id, ActionRequest(actionDescription, text))
			}

			//INIT ACTOR
			sendMessage(INIT_ACTION_TEXT)
			//FINISH INIT
			var state: CreateActionState = CreateActionState.ActionDescription

			for (message in channel) {
				when (message) {
					is ActorMessage.Say ->
						when (state) {
							CreateActionState.ActionDescription -> {
								actionDescription = message.text
								state = CreateActionState.ActionTags
								//send message: Enter duration
								sendMessage(TAGS)
								// flow is not finished
								message.unComplete()
							}
							CreateActionState.ActionTags -> {
								//received last item
								//call API
								when (createAction(message.text)) {
									is Right ->
										sendMessage(ACTION_SUCCESS)
									else ->
										sendMessage(ACTION_FAILED)
								}
								state = CreateActionState.Terminated
								message.complete()
							}

							CreateActionState.Terminated -> {
								message.completeExceptionally(IllegalStateException("We are done already!"))
							}
						}
				}
			}
		}
	}
}

sealed class CreateActionState {
	object ActionDescription : CreateActionState()
	object ActionTags : CreateActionState()
	object Terminated : CreateActionState()
}