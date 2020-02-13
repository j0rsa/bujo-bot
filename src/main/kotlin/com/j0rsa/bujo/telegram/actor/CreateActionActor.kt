package com.j0rsa.bujo.telegram.actor

import arrow.core.Either.Right
import com.j0rsa.bujo.telegram.api.model.*
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
const val CAN_NOT_BE_SKIPPED = "Cannot be skipped"
const val VALUES = "Insert mood value from 1 to 5 or /skip"

object CreateActionActor : Actor {
	fun descriptionExistMessage(s: String) = "Your description: $s. Enter action description or /skip"
	fun tagsExistMessage(tags: List<TagRequest>) =
		"Your tags: ${tags.joinToString(", ") { it.name }}. Enter tags or /skip"

	@UseExperimental(ObsoleteCoroutinesApi::class)
	override fun yield(ctx: ActorContext) = ctx.scope.actor<ActorMessage> {
		//INIT ACTOR
		var receiver: Receiver = ActorSayMessageReceiver.init(ctx)

		for (message in channel) {
			receiver = when (message) {
				is ActorMessage.Say -> receiver.say(message)
				is ActorMessage.Back -> receiver.back(message)
				is ActorMessage.Skip -> receiver.skip(message)
				is ActorMessage.Cancel -> receiver.cancel(message)
			}
		}
	}

	private data class ActorSayMessageReceiver(
		private val user: User,
		private val ctx: ActorContext,
		private var actionDescription: String = "",
		private var tags: List<TagRequest> = emptyList()
	) {

		private fun descriptionReceiver(): Receiver = object : LocalReceiver(cancel()) {
			override fun say(message: ActorMessage.Say): Receiver {
				actionDescription = message.text
				//send message: Enter duration
				sendMessage(TAGS)
				// flow is not finished
				message.unComplete()
				return tagsReceiver()
			}

			override fun back(message: ActorMessage.Back): Receiver = message.cancel()

			override fun skip(message: ActorMessage.Skip): Receiver = when {
				actionDescription.isEmpty() -> {
					sendMessage(CAN_NOT_BE_SKIPPED)
					this
				}
				else -> {
					sendMessage(TAGS)
					tagsReceiver()
				}
			}.also { message.unComplete() }
		}

		private fun tagsReceiver(): Receiver = object : LocalReceiver(cancel()) {
			override fun say(message: ActorMessage.Say): Receiver {
				tags = message.text.splitToTags()
				return createAction(message)
			}

			override fun back(message: ActorMessage.Back): Receiver {
				sendMessage(descriptionExistMessage(actionDescription))
				message.unComplete()
				return descriptionReceiver()
			}

			override fun skip(message: ActorMessage.Skip): Receiver {
				message.unComplete()
				sendMessage(CAN_NOT_BE_SKIPPED)
				return this
			}
		}

		private fun createAction(message: ActorMessage): Receiver {
			message.complete()
			when (createAction()) {
				is Right -> sendMessage(ACTION_SUCCESS)
				else -> sendMessage(ACTION_FAILED)
			}
			return TerminatedReceiver
		}

		abstract class LocalReceiver(private val cancelFun: (ActorMessage) -> Receiver) : Receiver {
			override fun cancel(message: ActorMessage.Cancel): Receiver = message.cancel()
			fun ActorMessage.cancel(): Receiver = cancelFun(this)
		}

		private fun cancel() = { message: ActorMessage ->
			sendMessage(ACTION_CANCELLED_TEXT)
			message.complete()
			TerminatedReceiver
		}

		fun sendMessage(text: String) = ctx.bot.sendMessage(ctx.chatId, text)

		private fun createAction() =
			ctx.client.createAction(user.id, ActionRequest(actionDescription, tags))

		private fun String.splitToTags() = this.split(",").map { TagRequest.fromString(it) }

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