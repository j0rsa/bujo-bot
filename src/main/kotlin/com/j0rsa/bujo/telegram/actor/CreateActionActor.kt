package com.j0rsa.bujo.telegram.actor

import arrow.core.Either.Right
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.User
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
	override fun yield(ctx: ActorContext) = with(ctx.scope) {
		actor<ActorMessage> {
			//INIT ACTOR
			val actor = Actor.init(ctx)
			//FINISH INIT
			var receiver: (ActorMessage.Say) -> Boolean = actor.receiver

			for (message in channel) {
				if (message is ActorMessage.Say) {
					receiver(message)
					receiver = actor.receiver
				}
			}
		}
	}

	private data class Actor(
		private val user: User,
		private val ctx: ActorContext,
		private var actionDescription: String = ""
	) {
		var receiver: (ActorMessage.Say) -> Boolean = descriptionReceiver()

		private fun descriptionReceiver() = { message: ActorMessage.Say ->
			receiver = tagsReceiver()
			actionDescription = message.text
			//send message: Enter duration
			sendMessage(TAGS)
			// flow is not finished
			message.unComplete()
		}

		private fun tagsReceiver() = { message: ActorMessage.Say ->
			receiver = terminatedReceiver()
			when (createAction(message.text)) {
				is Right -> sendMessage(ACTION_SUCCESS)
				else -> sendMessage(ACTION_FAILED)
			}
			message.complete()
		}

		private fun terminatedReceiver() = { message: ActorMessage.Say ->
			message.completeExceptionally(IllegalStateException("We are done already!"))
		}

		fun sendMessage(text: String) = ctx.bot.sendMessage(ctx.chatId, text)

		private fun createAction(text: String) =
			ctx.client.createAction(user.id, ActionRequest(actionDescription, text))

		companion object {
			fun init(ctx: ActorContext): Actor {
				val user = ctx.client.getUser(ctx.userId)
				val actor = Actor(user, ctx)
				actor.sendMessage(INIT_ACTION_TEXT)
				return actor
			}
		}
	}
}