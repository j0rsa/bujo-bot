package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.actor.ActorMessage
import com.j0rsa.bujo.telegram.actor.CreateActionActor
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

object BujoLogic : CoroutineScope by CoroutineScope(Dispatchers.Default) {
	private val userActors = mutableMapOf<Long, SendChannel<ActorMessage>>()
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

	fun handleActorMessage(update: Update) {
		update.message?.let { message ->
			message.from?.let { user ->
				message.text?.let { text ->
					launch {
						userActors[user.id]?.let { actorChannel ->
							val deferredFinished = CompletableDeferred<Boolean>()
							if (!actorChannel.isClosedForSend) {
								actorChannel.send(ActorMessage.Say(text, deferredFinished))
								if (deferredFinished.await()) {
									actorChannel.close()
									userActors.remove(message.chat.id)
								}
							}
						}
					}
				}
			}
		}
	}

	fun handleActorMessage(update: Update, command: ActorCommand) {
		update.message?.let { message ->
			message.from?.let { user ->
				launch {
					userActors[user.id]?.let { actorChannel ->
						val deferredFinished = CompletableDeferred<Boolean>()
						if (!actorChannel.isClosedForSend) {
							when (command) {
								is ActorCommand.Skip -> actorChannel.send(ActorMessage.Skip(deferredFinished))
								is ActorCommand.Back -> actorChannel.send(ActorMessage.Back(deferredFinished))
							}
							if (deferredFinished.await()) {
								actorChannel.close()
								userActors.remove(message.chat.id)
							}
						}
					}
				}
			}
		}
	}

	sealed class ActorCommand() {
		object Skip : ActorCommand()
		object Back : ActorCommand()
	}

	fun showHabits(bot: Bot, update: Update) {
		update.message?.let { message ->
			message.from?.let { user ->
				val trackerUser = TrackerClient.getUser(user.id)
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

	fun createAction(bot: Bot, update: Update) {
		update.message?.let { message ->
			message.from?.let {
				launch {
					userActors[it.id]?.close()
					userActors.put(
						it.id,
						CreateActionActor.yield(ActorContext(message.chat.id, it.id, bot, this))
					)
				}
			}
		}
	}

}

private fun List<HabitsInfo>.toHabitsInlineKeys(): List<List<InlineKeyboardButton>> =
	this.map { habitsInfo ->
		val streakCaption = if (habitsInfo.currentStreak > BigDecimal.ONE) "strike: ${habitsInfo.currentStreak}" else ""
		val habitCaption = "${habitsInfo.habit.name}  $streakCaption"
		listOf(
			InlineKeyboardButton("🔲" or "☑️"),
			InlineKeyboardButton(habitCaption)
		)
	}

private infix fun String.or(other: String) =
	if (Math.random() < 0.5) this else other
