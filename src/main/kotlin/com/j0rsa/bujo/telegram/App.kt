package com.j0rsa.bujo.telegram

import com.google.gson.Gson
import com.j0rsa.bujo.telegram.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.BujoLogic.ActorCommand.*
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.callbackQuery
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.dispatcher.message
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
				command("skip") { _, update -> BujoLogic.handleActorMessage(update, Skip) }
				command("back") { _, update -> BujoLogic.handleActorMessage(update, Back) }
				command("cancel") { _, update -> BujoLogic.handleActorMessage(update, Cancel) }
				message(ShowHabitsButtonFilter) { bot, update -> BujoLogic.showHabits(bot, update) }
				message(CreateActionButtonFilter) { bot, update -> BujoLogic.createAction(bot, update) }
				message(Filter.Text and ShowHabitsButtonFilter.not() and CreateActionButtonFilter.not()) { _, update ->
					val message = update.message ?: return@message
					val userId = BotUserId(message.from ?: return@message)
					val text = message.text ?: return@message

					BujoLogic.handleActorMessage(
						HandleActorMessage(ChatId(message), userId, text)
					)
				}
				callbackQuery(CALLBACK_ADD_VALUE) { bot, update ->
					val userId = BotUserId(update.callbackQuery?.from ?: return@callbackQuery)
					val message = update.callbackQuery?.message ?: return@callbackQuery
					val data = parse(update.callbackQuery?.data ?: return@callbackQuery, CALLBACK_ADD_VALUE)
					val chatId = ChatId(message)
					val messageId = message.messageId
					bot.deleteMessage(chatId.value, messageId)
					BujoLogic.addValue(CallbackMessage(BujoBot(bot), userId, chatId, data))
				}
				callbackQuery(CALLBACK_ACTOR_TEMPLATE) { bot, update ->
					val userId = BotUserId(update.callbackQuery?.from ?: return@callbackQuery)
					val message = update.callbackQuery?.message ?: return@callbackQuery
					val data =
						parse(update.callbackQuery?.data ?: return@callbackQuery, CALLBACK_ACTOR_TEMPLATE)
					val chatId = ChatId(message)
					val messageId = message.messageId
					bot.deleteMessage(chatId.value, messageId)

					BujoLogic.handleActorMessage(HandleActorMessage(chatId, userId, data))
				}
			}
		}
		bot.startPolling()
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