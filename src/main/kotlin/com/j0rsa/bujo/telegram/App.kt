package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.BujoLogic.ActorCommand.*
import com.j0rsa.bujo.telegram.BujoLogic.addValue
import com.j0rsa.bujo.telegram.BujoLogic.handleActorMessage
import me.ivmg.telegram.Bot
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.callbackQuery
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.dispatcher.message
import me.ivmg.telegram.entities.CallbackQuery
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.User
import me.ivmg.telegram.extensions.filters.Filter
import okhttp3.logging.HttpLoggingInterceptor

/**
 * @author red
 * @since 02.02.20
 */

class App {
	fun run() {
		val bot = bot {
			token = Config.app.token
			logLevel = HttpLoggingInterceptor.Level.NONE
			dispatch {
				command("start") { bot, update -> BujoLogic.registerTelegramUser(bot, update) }
				command("habits") { bot, update -> BujoLogic.showHabits(bot, update) }
				command("skip") { _, update -> handleActorMessage(update, Skip) }
				command("back") { _, update -> handleActorMessage(update, Back) }
				command("cancel") { _, update -> handleActorMessage(update, Cancel) }
				message(ShowHabitsButtonFilter) { bot, update -> BujoLogic.showHabits(bot, update) }
				message(CreateActionButtonFilter) { bot, update -> BujoLogic.createAction(bot, update) }
				message(Filter.Text and ShowHabitsButtonFilter.not() and CreateActionButtonFilter.not()) { _, update ->
					val message = update.message ?: return@message
					val userId = BotUserId(message.from ?: return@message)
					val text = message.text ?: return@message

					handleActorMessage(HandleActorMessage(userId, ChatId(message), text))
				}
				callbackQuery(CALLBACK_ADD_VALUE) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					deleteMessage(bot, message)
					addValueCallback(callbackQuery, message, bot)
				}
				callbackQuery(CALLBACK_ACTOR_TEMPLATE) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					deleteMessage(bot, message)
					actorsCallback(callbackQuery, message)
				}
				callbackQuery(CALLBACK_VIEW_ACTION) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
//					deleteMessage(bot, message)
					editActionCallback(callbackQuery, message, bot)
				}
			}
		}
		bot.startPolling()
	}

	private fun addValueCallback(callbackQuery: CallbackQuery, message: Message, bot: Bot) {
		val userId = BotUserId(callbackQuery.from)
		val data = parse(callbackQuery.data, CALLBACK_ADD_VALUE)

		addValue(CallbackMessage(BujoBot(bot), userId, ChatId(message), data))
	}

	private fun actorsCallback(callbackQuery: CallbackQuery, message: Message) {
		val userId = BotUserId(callbackQuery.from)
		val data = parse(callbackQuery.data, CALLBACK_ACTOR_TEMPLATE)

		handleActorMessage(HandleActorMessage(userId, ChatId(message), data))
	}

	private fun editActionCallback(callbackQuery: CallbackQuery, message: Message, bot: Bot) {
		val userId = BotUserId(callbackQuery.from)
		val data = parse(callbackQuery.data, CALLBACK_VIEW_ACTION)

		BujoLogic.editAction(CallbackMessage(BujoBot(bot), userId, ChatId(message), data))
	}

	private fun deleteMessage(bot: Bot, message: Message) {
		bot.deleteMessage(message.chat.id, message.messageId)
	}

	object ShowHabitsButtonFilter : Filter {
		override fun Message.predicate(): Boolean =
			text == BujoTalk.withLanguage(from?.languageCode).showHabitsButton
	}

	object CreateActionButtonFilter : Filter {
		override fun Message.predicate(): Boolean =
			text == BujoTalk.withLanguage(from?.languageCode).createActionButton
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			App().run()
		}
	}
}

inline class BotUserId(val value: Long) {
	constructor(user: User) : this(user.id)
}

inline class ChatId(val value: Long) {
	constructor(message: Message) : this(message.chat.id)
}

fun parse(text: String, template: String): String = text.substringAfter("$template:")