package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import com.j0rsa.bujo.telegram.api.model.Action
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.User
import com.j0rsa.bujo.telegram.editAction
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.ivmg.telegram.entities.ReplyMarkup

const val ACTION_NOT_FOUND = "Action not found"

object EditActionActor {
	fun initActionEdit(action: Action) = """
|Here's your action, you can edit it: ${action.description} 
|🏷${action.tags.joinToString(", \uD83C\uDFF7") { it.name }}
|${if (action.values.isNotEmpty()) "values: " else ""} ${action.values.map { "${it.name} - ${it.value}" }.joinToString("; ")}"""
		.trimMargin()

	fun yield(
		actorMessage: ActorMessage.Say,
		ctx: ActorContext
	): SendChannel<ActorMessage> = ctx.scope.actor {
		var receiver: Receiver = MessageReceiver.init(actorMessage, ctx)
		for (message in channel) {
			receiver = when (message) {
				is ActorMessage.Say -> receiver.say(message)
				is ActorMessage.Back -> receiver.back(message)
				is ActorMessage.Skip -> receiver.skip(message)
				is ActorMessage.Cancel -> receiver.cancel(message)
			}
		}
	}

	private data class MessageReceiver(
		val user: User,
		val action: Action,
		val ctx: ActorContext
	) {
		fun sendMessage(text: String, replyMarkup: ReplyMarkup? = null) =
			ctx.bot.sendMessage(ctx.chatId, text, replyMarkup = replyMarkup)

		companion object {
			fun init(
				message: ActorMessage.Say,
				ctx: ActorContext
			): Receiver {
				val user = ctx.client.getUser(ctx.userId)
				val actionId = ActionId.fromString(message.text)
				return when (val result = ctx.client.getAction(user.id, actionId)) {
					is Either.Left -> {
						message.complete()
						ctx.bot.sendMessage(ctx.chatId, ACTION_NOT_FOUND)
						TerminatedReceiver
					}
					is Either.Right -> {
						message.complete()
						val actor = MessageReceiver(user, result.b, ctx)
						actor.sendMessage(initActionEdit(result.b), editAction(result.b))
						TerminatedReceiver
					}
				}
			}
		}
	}
}