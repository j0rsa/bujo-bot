package com.j0rsa.bujo.telegram.actor

import arrow.core.Either.Right
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.User
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor

/**
 * @author red
 * @since 09.02.20
 */
const val INIT_ACTION_TEXT = """You are creating an action\n
                    Enter action description"""
const val ACTION_CANCELLED_TEXT = "You canceled action creation"
const val TAGS = "Enter actions tags (comma separated) or go /back"
const val ACTION_SUCCESS = "Action was registered"
const val ACTION_FAILED = "Action was not registered \uD83D\uDE22"
const val NOT_SKIPPABLE = "Cannot be skipped"

object CreateActionActor : Actor {
	fun descriptionExistMessage(s: String) = "Your description: $s. Enter action description or /skip"

	@UseExperimental(ObsoleteCoroutinesApi::class)
	override fun yield(ctx: ActorContext) = ctx.scope.actor<ActorMessage> {
		//INIT ACTOR
		var receiver: Receiver = ActorSayMessageReceiver.init(ctx)

		for (message in channel) {
			receiver = when (message) {
				is ActorMessage.Say -> receiver.say(message)
				is ActorMessage.Back -> receiver.back(message)
				is ActorMessage.Skip -> receiver.skip(message)
			}
		}
	}

	private data class ActorSayMessageReceiver(
		private val user: User,
		private val ctx: ActorContext,
		private var actionDescription: String = ""
	) {

		private fun descriptionReceiver(): Receiver = object : Receiver {
			override fun say(message: ActorMessage.Say): Receiver {
				actionDescription = message.text
				//send message: Enter duration
				sendMessage(TAGS)
				// flow is not finished
				message.unComplete()
				return tagsReceiver()
			}

			override fun back(message: ActorMessage.Back): Receiver {
				message.complete()
				sendMessage(ACTION_CANCELLED_TEXT)
				return this
			}

			override fun skip(message: ActorMessage.Skip): Receiver =
				if (actionDescription.isEmpty()) {
					sendMessage(NOT_SKIPPABLE)
					this
				} else {
					sendMessage(TAGS)
					tagsReceiver()
				}

		}

		private fun tagsReceiver(): Receiver = object : Receiver {
			override fun say(message: ActorMessage.Say): Receiver {
				when (createAction(message.text)) {
					is Right -> sendMessage(ACTION_SUCCESS)
					else -> sendMessage(ACTION_FAILED)
				}
				message.complete()
				return terminatedReceiver()
			}

			override fun back(message: ActorMessage.Back): Receiver {
				sendMessage(descriptionExistMessage(actionDescription))
				message.unComplete()
				return descriptionReceiver()
			}

			override fun skip(message: ActorMessage.Skip): Receiver {
				sendMessage(NOT_SKIPPABLE)
				message.unComplete()
				return this
			}
		}

		private fun terminatedReceiver(): Receiver = object : Receiver {
			override fun say(message: ActorMessage.Say): Receiver {
				message.completeExceptionally(IllegalStateException("We are done already!"))
				return this
			}

			override fun back(message: ActorMessage.Back): Receiver {
				message.completeExceptionally(IllegalStateException("We are done already!"))
				return this
			}

			override fun skip(message: ActorMessage.Skip): Receiver {
				message.completeExceptionally(IllegalStateException("We are done already!"))
				return this
			}

		}

		fun sendMessage(text: String) = ctx.bot.sendMessage(ctx.chatId, text)

		private fun createAction(text: String) =
			ctx.client.createAction(user.id, ActionRequest(actionDescription, text))

		companion object {
			fun init(ctx: ActorContext): Receiver {
				val user = ctx.client.getUser(ctx.userId)
				val actor = ActorSayMessageReceiver(user, ctx)
				actor.sendMessage(INIT_ACTION_TEXT)
				return actor.descriptionReceiver()
			}
		}
	}
}

interface Receiver {
	fun say(message: ActorMessage.Say): Receiver
	fun back(message: ActorMessage.Back): Receiver
	fun skip(message: ActorMessage.Skip): Receiver
}