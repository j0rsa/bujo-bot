package com.j0rsa.bujo.telegram.actor

import arrow.core.Either.Right
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

			//INIT ACTOR
			ctx.bot.sendMessage(
				chatId,
				INIT_ACTION_TEXT
			)
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
								ctx.bot.sendMessage(chatId, TAGS)
								// flow is not finished
								message.deferred.complete(false)
							}
							CreateActionState.ActionTags -> {
								//received last item
								val actionTags = message.text.split(",").map { TagRequest.fromString(it) }
								//call API
								when (ctx.client.createAction(
									user.id, ActionRequest(
										actionDescription,
										actionTags
									)
								)) {
									is Right ->
										ctx.bot.sendMessage(chatId, ACTION_SUCCESS)
									else ->
										ctx.bot.sendMessage(chatId, ACTION_FAILED)
								}
								state = CreateActionState.Terminated
								message.deferred.complete(true)
							}

							CreateActionState.Terminated -> {
								message.deferred.completeExceptionally(IllegalStateException("We are done already!"))
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