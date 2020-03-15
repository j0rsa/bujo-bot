package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.BujoMarkup.permanentMarkup
import com.j0rsa.bujo.telegram.actor.*
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
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
import java.util.*

/**
 * @author red
 * @since 09.02.20
 */

@OptIn(ExperimentalCoroutinesApi::class)
object BujoLogic : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val userActors = mutableMapOf<BotUserId, SendChannel<ActorMessage>>()
    fun checkBackendStatus(bot: Bot, message: Message) {
        val text = if (TrackerClient.health()) {
            "😀"
        } else {
            "\uD83D\uDE30"
        }
        bot.sendMessage(
            message.chat.id,
            "${BujoTalk.withLanguage(message.from?.languageCode).statusMessage} $text"
        )
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
                    Status.CREATED -> BujoTalk.withLanguage(it.languageCode).welcome
                    Status.OK -> BujoTalk.withLanguage(it.languageCode).welcomeBack
                    else -> BujoTalk.withLanguage(it.languageCode).genericError
                }
				bot.sendMessage(
                    message.chat.id,
                    text,
                    replyMarkup = permanentMarkup(message.from?.languageCode)
                )
			}
		}
	}

	fun handleActorMessage(message: HandleActorMessage) {
		launch {
			userActors[message.userId]?.let { actorChannel ->
				val deferredFinished = CompletableDeferred<Boolean>()
				if (!actorChannel.isClosedForSend) {
					actorChannel.send(ActorMessage.Say(message.text.trim(), deferredFinished))
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

    fun createAction(bot: Bot, update: Update) = initNewActor(bot, update) { _, _, ctx ->
        CreateActionActor.yield(
            CreateActionState(ctx)
        )
    }

    fun addValue(message: CallbackMessage) {
        launch {
            userActors[message.userId]?.close()
            userActors[message.userId] = AddValueActor.yield(
                AddValueState(
                    message.toContext(this),
                    ActionId(UUID.fromString(message.callBackData))
                )
            )
        }
    }

    fun showSettings(bot: Bot, update: Update) {
        update.message?.let {
            val withLanguage = BujoTalk.withLanguage(it.from?.languageCode)
            bot.sendMessage(
                ChatId(it).value,
                withLanguage.settingsMessage,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton(
                                withLanguage.checkBackendButton,
                                callbackData = CALLBACK_SETTINGS_CHECK_BACKEND
                            )
                        )
                    )
                )
            )
        }
    }

    fun createHabit(bot: Bot, update: Update) = initNewActor(bot, update) { _, _, ctx ->
        HabitActor.yield(
            CreateHabitState(ctx)
        )
    }

    private fun initNewActor(
        bot: Bot,
        update: Update,
        init: (user: User, message: Message, ctx: ActorContext) -> SendChannel<ActorMessage>
    ) {
        update.message?.let { message ->
            message.from?.let { user: User ->
                launch {
                    val userId = BotUserId(user)
                    userActors[userId]?.close()
                    userActors[userId] = init(
                        user,
                        message,
                        ActorContext(ChatId(message), BotUserId(user), BujoBot(bot), this)
                    )
                }
            }
        }
    }

//	fun editAction(message: CallbackMessage) {
//		launch {
//			val deferredFinished = CompletableDeferred<Boolean>()
//			val actorMessage = ActorMessage.Say(message.callBackData, deferredFinished)
//			userActors[message.userId]?.close()
//			val channel = EditActionActor.yield(actorMessage, message.toContext(this))
//			if (deferredFinished.await()) {
//				channel.close()
//			} else {
//				userActors[message.userId] = channel
//			}
//		}
//	}

}

sealed class BotMessage(
	private val bot: BujoBot,
	private val chatId: ChatId,
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
