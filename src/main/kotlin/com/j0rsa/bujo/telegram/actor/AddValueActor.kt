package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.valueMarkup
import com.j0rsa.bujo.telegram.valueTypeMarkup
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.ivmg.telegram.entities.ReplyMarkup

const val INIT_ADD_ACTION_VALUE = """You are adding a value for the action\n
Select type of value"""
const val VALUE_CANCELLED_TEXT = "You canceled value creation"
const val ADD_VALUE_ACTION_FAILED = "Value was not registered \uD83D\uDE22"
const val ADD_VALUE_ACTION_SUCCESS = "Value was registered"

object AddValueActor {
	fun defaultNameMessage(type: ValueType) = "Default name ${type.name}. Enter name or /skip or /back"
	fun String.notEmptyMessage() = "Name is $this. Enter name or /skip or /back"
	fun typeExistMessage(type: ValueType) = "Your value type: ${type.name}. Enter type or /skip"
	fun valueMessage(name: String) = "Enter value for $name or go /back"
	@ObsoleteCoroutinesApi
	fun yield(
		data: String,
		ctx: ActorContext
	): SendChannel<ActorMessage> = ctx.scope.actor<ActorMessage> {
		var receiver: Receiver = MessageReceiver.init(data, ctx)
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
		private val user: User,
		private val ctx: ActorContext,
		private val actionId: ActionId
	) {
		lateinit var type: ValueType
		lateinit var name: String
		lateinit var value: String

		private fun typeReceiver(): Receiver = object : LocalReceiver(cancel()) {
			override fun say(message: ActorMessage.Say): Receiver {
				message.unComplete()
				type = ValueType.valueOf(message.text)
				sendMessage(defaultNameMessage(type))
				return nameReceiver()
			}

			override fun back(message: ActorMessage.Back): Receiver = message.cancel()

			override fun skip(message: ActorMessage.Skip): Receiver = when (isTypeInitialized()) {
				true -> {
					sendMessage(if (isNameInitialized()) name.notEmptyMessage() else defaultNameMessage(type))
					nameReceiver()
				}
				false -> {
					sendMessage(CAN_NOT_BE_SKIPPED)
					this
				}
			}.also { message.unComplete() }
		}

		private fun nameReceiver(): Receiver = object : LocalReceiver(cancel()) {
			override fun say(message: ActorMessage.Say): Receiver {
				name = message.text
				sendMessage(valueMessage(name), valueMarkup(type))
				message.unComplete()
				return valueReceiver()
			}

			override fun back(message: ActorMessage.Back): Receiver {
				sendMessage(typeExistMessage(type), valueTypeMarkup())
				message.unComplete()
				return typeReceiver()
			}

			override fun skip(message: ActorMessage.Skip): Receiver {
				message.unComplete()
				if (!isNameInitialized()) name = type.name
				sendMessage(valueMessage(name), valueMarkup(type))
				return valueReceiver()
			}
		}

		private fun valueReceiver(): Receiver = object : LocalReceiver(cancel()) {
			override fun say(message: ActorMessage.Say): Receiver {
				message.complete()
				value = message.text
				when (val result = addValue()) {
					is Either.Right -> valueAddedMessage(result.b)
					else -> sendMessage(ADD_VALUE_ACTION_FAILED)
				}
				return TerminatedReceiver
			}

			override fun back(message: ActorMessage.Back): Receiver {
				sendMessage(name.notEmptyMessage())
				message.unComplete()
				return nameReceiver()
			}

			override fun skip(message: ActorMessage.Skip): Receiver {
				message.unComplete()
				sendMessage(CAN_NOT_BE_SKIPPED)
				return this
			}
		}

		private fun isNameInitialized() = ::name.isInitialized
		private fun isTypeInitialized() = ::type.isInitialized

		abstract class LocalReceiver(private val cancelFun: (ActorMessage) -> Receiver) : Receiver {
			override fun cancel(message: ActorMessage.Cancel): Receiver = message.cancel()
			fun ActorMessage.cancel(): Receiver = cancelFun(this)
		}

		private fun cancel() = { message: ActorMessage ->
			sendMessage(VALUE_CANCELLED_TEXT)
			message.complete()
			TerminatedReceiver
		}

		private fun addValue() = ctx.client.addValue(user.id, actionId, Value(type, value, name))

		fun sendMessage(text: String, replyMarkup: ReplyMarkup? = null) =
			ctx.bot.sendMessage(ctx.chatId, text, replyMarkup = replyMarkup)

		fun valueAddedMessage(newActionId: ActionId) =
			ctx.bot.valueAddedMessage(ctx.chatId, newActionId)

		companion object {
			fun init(
				data: String,
				ctx: ActorContext
			): Receiver {
				val user = ctx.client.getUser(ctx.userId)
				val actor = MessageReceiver(user, ctx, ActionId.fromString(data))
				actor.sendMessage(INIT_ADD_ACTION_VALUE, valueTypeMarkup())
				return actor.typeReceiver()
			}
		}
	}
}