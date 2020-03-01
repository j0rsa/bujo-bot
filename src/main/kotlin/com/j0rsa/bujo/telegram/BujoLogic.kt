package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.actor.ActorMessage
import com.j0rsa.bujo.telegram.actor.AddValueActor
import com.j0rsa.bujo.telegram.actor.CreateActionActor
import com.j0rsa.bujo.telegram.actor.EditActionActor
import com.j0rsa.bujo.telegram.api.TrackerClient
import com.j0rsa.bujo.telegram.api.model.CreateUserRequest
import com.j0rsa.bujo.telegram.api.model.HabitsInfo
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.*
import org.http4k.core.Status
import java.math.BigDecimal

/**
 * @author red
 * @since 09.02.20
 */

@ExperimentalCoroutinesApi
object BujoLogic : CoroutineScope by CoroutineScope(Dispatchers.Default) {
	private val userActors = mutableMapOf<BotUserId, SendChannel<ActorMessage>>()
	fun showMenu(bot: Bot, update: Update) {
		update.message?.let { message ->
			val text = if (TrackerClient.health()) {
				"😀"
			} else {
				"\uD83D\uDE30"
			}
			bot.sendMessage(
				message.chat.id,
				text
			)
		}
	}

	fun selectLanguage(bot: Bot, update: Update) {
		update.message?.let { message ->
			bot.sendMessage(
				message.chat.id,
				text = "Language",
				replyMarkup = InlineKeyboardMarkup(
					listOf(
						BujoTalk.getSupportedLanguageCodesWithFlags().map {
							InlineKeyboardButton(it.second, callbackData = it.first.name)
						}
					)
				)

			)
		}
	}

	fun showCurrentLanguage(bot: Bot, update: Update) {
		update.message?.let { message ->
			bot.sendMessage(
				message.chat.id,
				BujoTalk.getSupportedLanguageCodesWithFlags().first { it.first == Language.valueOf(message.from!!.languageCode!!.toUpperCase()) }.second
			)
		}
	}

	fun registerTelegramUser(bot: Bot, update: Update) {
		update.message?.let { message ->
			message.from?.let {
				val (_, status) = TrackerClient.createUser(
					CreateUserRequest(
						telegramId = it.id,
						firstName = it.firstName,
						lastName = it.lastName ?: "",
						language = it.languageCode ?: Config.app.defaultLanguage.toLowerCase()
					)
				)
				val text = when (status) {
					Status.CREATED -> {
						BujoTalk.withLanguage(it.languageCode).welcome
					}
					Status.OK -> {
						BujoTalk.withLanguage(it.languageCode).welcomeBack
					}
					else -> {
						BujoTalk.withLanguage(it.languageCode).genericError
					}
				}
				bot.sendMessage(
					message.chat.id,
					text,
					replyMarkup = KeyboardReplyMarkup(
						KeyboardButton(BujoTalk.withLanguage(message.from?.languageCode).showHabitsButton),
						KeyboardButton(BujoTalk.withLanguage(message.from?.languageCode).createActionButton),
						resizeKeyboard = true
					)
				)
			}
		}
	}

	fun handleActorMessage(message: HandleActorMessage) {
		launch {
			userActors[message.userId]?.let { actorChannel ->
				val deferredFinished = CompletableDeferred<Boolean>()
				if (!actorChannel.isClosedForSend) {
					actorChannel.send(ActorMessage.Say(message.text, deferredFinished))
					if (deferredFinished.await()) {
						actorChannel.close()
						userActors.remove(message.userId)
					}
				}
			}
		}
	}

	fun handleActorMessage(update: Update, command: ActorCommand) {
		update.message?.let { message ->
			message.from?.let { user ->
				launch {
					val userId = BotUserId(user)
					userActors[userId]?.let { actorChannel ->
						val deferredFinished = CompletableDeferred<Boolean>()
						if (!actorChannel.isClosedForSend) {
							when (command) {
								is ActorCommand.Skip -> actorChannel.send(ActorMessage.Skip(deferredFinished))
							}
							if (deferredFinished.await()) {
								actorChannel.close()
								userActors.remove(userId)
							}
						}
					}
				}
			}
		}
	}

	sealed class ActorCommand {
		object Skip : ActorCommand()
	}

	fun showHabits(bot: Bot, update: Update) {
		update.message?.let { message ->
			message.from?.let { user ->
				val trackerUser = TrackerClient.getUser(BotUserId(user.id))
				val habits = TrackerClient.getHabits(trackerUser.id)
				bot.sendMessage(
					message.chat.id,
					text = BujoTalk.withLanguage(user.languageCode).showHabitsMessage,
					replyMarkup = InlineKeyboardMarkup(
						habits.toHabitsInlineKeys()
					)
				)
			}
		}

	}

	@ObsoleteCoroutinesApi
	fun createAction(bot: Bot, update: Update) {
		update.message?.let { message ->
			message.from?.let { user: User ->
				launch {
					val userId = BotUserId(user)
					userActors[userId]?.close()
					userActors[userId] = CreateActionActor.yield(
						ActorContext(ChatId(message), userId, BujoBot(bot), this)
					)
				}
			}
		}
	}

	fun addValue(message: CallbackMessage) {
		launch {
			userActors[message.userId]?.close()
			userActors[message.userId] = AddValueActor.yield(
				message.callBackData,
				message.toContext(this)
			)
		}
	}

	fun editAction(message: CallbackMessage) {
		launch {
			val deferredFinished = CompletableDeferred<Boolean>()
			val actorMessage = ActorMessage.Say(message.callBackData, deferredFinished)
			userActors[message.userId]?.close()
			val channel = EditActionActor.yield(actorMessage, message.toContext(this))
			if (deferredFinished.await()) {
				channel.close()
			} else {
				userActors[message.userId] = channel
			}
		}
	}

}

sealed class BotMessage(
	val bot: BujoBot,
	val chatId: ChatId,
	val userId: BotUserId
) {
	class CallbackMessage(
		bot: BujoBot,
		userId: BotUserId,
		chatId: ChatId,
		val callBackData: String
	) : BotMessage(bot, chatId, userId)

	fun toContext(scope: CoroutineScope) = ActorContext(chatId, userId, bot, scope)
}

class HandleActorMessage(
	val userId: BotUserId,
	val chatId: ChatId,
	val text: String
)

private fun List<HabitsInfo>.toHabitsInlineKeys(): List<List<InlineKeyboardButton>> =
	this.map { habitsInfo ->
		val streakCaption = if (habitsInfo.currentStreak > BigDecimal.ONE) "strike: ${habitsInfo.currentStreak}" else ""
		val habitCaption = "${habitsInfo.habit.name}  $streakCaption"
		listOf(
			InlineKeyboardButton("🔲" or "☑️"),
			InlineKeyboardButton(habitCaption)
		)
	}

private infix fun String.or(other: String) = if (Math.random() < 0.5) this else other
