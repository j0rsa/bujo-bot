package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.bot.*
import com.j0rsa.bujo.telegram.bot.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.bot.BujoLogic.addValue
import com.j0rsa.bujo.telegram.bot.BujoLogic.handleUserActorSayMessage
import com.j0rsa.bujo.telegram.bot.BujoLogic.handleUserActorSkipMessage
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
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
import java.util.*

/**
 * @author red
 * @since 02.02.20
 */

class App: WithLogger() {
	fun run() {
		val bot = bot {
			token = Config.app.token
			logLevel = HttpLoggingInterceptor.Level.valueOf(Config.app.sdkLoggingLevel)
			dispatch {
				command("start") { bot, update ->
					BujoLogic.registerTelegramUser(
						bot,
						ChatId(update.message ?: return@command),
						update.message?.from ?: return@command
					) }
				command("skip") { _, update ->
					handleUserActorSkipMessage(update.message?.from ?: return@command) }
				command("habits") { bot, update ->
					BujoLogic.showHabits(
						bot,
						ChatId(update.message ?: return@command),
						update.message?.from ?: return@command
					) }
				message(CreateHabitButtonFilter) { bot, update ->
					BujoLogic.createHabit(
						bot,
						ChatId(update.message ?: return@message),
						update.message?.from ?: return@message
					) }
				callbackQuery(CALLBACK_CREATE_HABIT_BUTTON) { bot, update ->
					BujoLogic.createHabit(
						bot,
						ChatId(update.callbackQuery?.message ?: return@callbackQuery),
						update.callbackQuery?.from ?: return@callbackQuery
					) }
				message(ShowHabitsButtonFilter) { bot, update -> BujoLogic.showHabits(
					bot,
					ChatId(update.message ?: return@message),
					update.message?.from ?: return@message
				) }
				message(CreateActionButtonFilter) { bot, update ->
					BujoLogic.createAction(
						bot,
						ChatId(update.message ?: return@message),
						update.message?.from ?: return@message
					) }
				message(SettingsButtonFilter) { bot, update ->
					BujoLogic.showSettings(
						bot,
						ChatId(update.message ?: return@message),
						update.message?.from ?: return@message
					) }
				message(Filter.Text and notTextButton()) { _, update ->
					val message = update.message ?: return@message
					val userId = BotUserId(message.from ?: return@message)
					val text = message.text ?: return@message
					handleUserActorSayMessage(
						HandleActorMessage(userId, ChatId(message), text)
					)
				}
				callbackQuery("") { _, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					logger.debug("Received callback: ${callbackQuery.data}")
				}
				callbackQuery(CALLBACK_ADD_VALUE) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					deleteMessage(bot, message)
					val data = parse(callbackQuery.data, CALLBACK_ADD_VALUE)
					addValueCallback(
						bot,
						ChatId(message),
						callbackQuery.from,
						data
					)
				}
				callbackQuery(CALLBACK_ACTOR_TEMPLATE) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					deleteMessage(bot, message)
					actorsCallback(callbackQuery, message)
				}
				callbackQuery(CALLBACK_SETTINGS_CHECK_BACKEND) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					deleteMessage(bot, message)
					BujoLogic.checkBackendStatus(bot, message)
				}
				callbackQuery(CALLBACK_VIEW_HABIT) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val habitId = parse(callbackQuery.data, CALLBACK_VIEW_HABIT)
					callbackQuery.message ?: return@callbackQuery
					BujoLogic.showHabit(bot, callbackQuery, UUID.fromString(habitId))
				}
				callbackQuery(CALLBACK_CONFIRM_DELETE_HABIT) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val habitId = parse(callbackQuery.data, CALLBACK_CONFIRM_DELETE_HABIT)
					callbackQuery.message ?: return@callbackQuery
					BujoLogic.showHabitDeleteConfirmation(bot, callbackQuery, UUID.fromString(habitId))
				}
				callbackQuery(CALLBACK_NO_BUTTON) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					deleteMessage(bot, message)
				}
				callbackQuery(CALLBACK_PERFORM_DELETE_HABIT) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					val habitId = parse(callbackQuery.data, CALLBACK_PERFORM_DELETE_HABIT)
					deleteMessage(bot, message)
					BujoLogic.deleteHabit(bot, callbackQuery, UUID.fromString(habitId))
				}
				callbackQuery(CALLBACK_ADD_FAST_HABIT_ACTION_BUTTON) { bot, update ->
					val callbackQuery = update.callbackQuery ?: return@callbackQuery
					val message = callbackQuery.message ?: return@callbackQuery
					val habitId = parse(callbackQuery.data, CALLBACK_ADD_FAST_HABIT_ACTION_BUTTON)
					deleteMessage(bot, message)
					BujoLogic.addFastHabitActionFromQuery(
						bot,
						ChatId(message),
						message.from ?: return@callbackQuery,
						UUID.fromString(habitId)
					)
				}
//				callbackQuery(CALLBACK_VIEW_ACTION) { bot, update ->
//					val callbackQuery = update.callbackQuery ?: return@callbackQuery
//					val message = callbackQuery.message ?: return@callbackQuery
//					deleteMessage(bot, message)
//					editActionCallback(callbackQuery, message, bot)
//				}
			}
		}
		bot.startPolling()
		logger.info("Bot started listening for updates")
	}

//	private fun editActionCallback(callbackQuery: CallbackQuery, message: Message, bot: Bot) {
//		val userId = BotUserId(callbackQuery.from)
//		val data = parse(callbackQuery.data, CALLBACK_VIEW_ACTION)
//
//		BujoLogic.editAction(CallbackMessage(BujoBot(bot), userId, ChatId(message), data))
//	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			App().run()
		}

		private fun actorsCallback(callbackQuery: CallbackQuery, message: Message) {
			val userId = BotUserId(callbackQuery.from)
			val data = parse(callbackQuery.data, CALLBACK_ACTOR_TEMPLATE)

			handleUserActorSayMessage(
				HandleActorMessage(
					userId,
					ChatId(message),
					data
				)
			)
		}

		private fun deleteMessage(bot: Bot, message: Message) {
			bot.deleteMessage(message.chat.id, message.messageId)
		}

		private fun addValueCallback(bot: Bot, chatId: ChatId, user: User, data: String) {
			addValue(
				CallbackMessage(
					BujoBot(bot),
					BotUserId(user),
					data
				),
				chatId,
				user
			)
		}

		private fun notTextButton() =
			listOf(
				ShowHabitsButtonFilter,
				CreateHabitButtonFilter,
				CreateActionButtonFilter,
				SettingsButtonFilter
			).fold(Filter.All as Filter, { acc, filter -> acc and filter.not() })

		object CreateHabitButtonFilter : Filter {
			override fun Message.predicate(): Boolean =
				text == BujoTalk.withLanguage(from?.languageCode).createHabitButton
		}

		object ShowHabitsButtonFilter : Filter {
			override fun Message.predicate(): Boolean =
				text == BujoTalk.withLanguage(from?.languageCode).showHabitsButton
		}

		object CreateActionButtonFilter : Filter {
			override fun Message.predicate(): Boolean =
				text == BujoTalk.withLanguage(from?.languageCode).createActionButton
		}

		object SettingsButtonFilter : Filter {
			override fun Message.predicate(): Boolean =
				text == BujoTalk.withLanguage(from?.languageCode).settingsButton
		}

		private fun parse(text: String, template: String): String = text.substringAfter("$template:").trim()
	}
}
