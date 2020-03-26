package com.j0rsa.bujo.telegram.bot

import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.toIO
import arrow.fx.handleError
import com.j0rsa.bujo.telegram.Config
import com.j0rsa.bujo.telegram.actor.*
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.TrackerClient
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.bot.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.bot.Markup.createdActionMarkup
import com.j0rsa.bujo.telegram.bot.Markup.habitCreatedMarkup
import com.j0rsa.bujo.telegram.bot.Markup.habitMarkup
import com.j0rsa.bujo.telegram.bot.Markup.newHabitMarkup
import com.j0rsa.bujo.telegram.bot.Markup.noYesMarkup
import com.j0rsa.bujo.telegram.bot.Markup.permanentMarkup
import com.j0rsa.bujo.telegram.bot.Markup.settingsMarkup
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Language
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.*
import org.http4k.core.Status
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KProperty1

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
                BujoTalk.getSupportedLanguageCodesWithFlags()
                    .first { it.first == Language.valueOf(message.from!!.languageCode!!.toUpperCase()) }.second
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

    fun handleUserActorSayMessage(
        message: HandleActorMessage
    ) {
        userActors[message.userId]?.let { channel ->
            handleSayActorMessage(message.text.trim(), channel)
        }
    }

    fun handleSayActorMessage(
        message: String,
        channel: SendChannel<ActorMessage>
    ) {
        launch {
            if (!channel.isClosedForSend) {
                channel.send(ActorMessage.Say(message))
            }
        }
    }

    fun handleUserActorSkipMessage(update: Update) {
        update.message?.let { message ->
            message.from?.let { user ->
                launch {
                    val userId = BotUserId(user)
                    userActors[userId]?.let { actorChannel ->
                        if (!actorChannel.isClosedForSend) {
                            actorChannel.send(ActorMessage.Skip)
                        }
                    }
                }
            }
        }
    }

    fun showHabits(bot: Bot, update: Update) {
        update.message?.let { message ->
            message.from?.let { user ->
                IO.fx {
                    val (trackerUser) = TrackerClient.getUser(BotUserId(user.id))
                    val (habits) = TrackerClient.getHabits(trackerUser.id)
                    with(BujoTalk.withLanguage(user.languageCode)) {
                        if (habits.isEmpty()) {
                            bot.sendMessage(
                                message.chat.id,
                                text = noHabitsRegistered,
                                replyMarkup = newHabitMarkup(user.languageCode)
                            )
                        } else {
                            bot.sendMessage(
                                message.chat.id,
                                text = showHabitsMessage,
                                replyMarkup = InlineKeyboardMarkup(
                                    habits.toHabitsInlineKeys()
                                )
                            )
                        }
                    }
                }.handleError {
                    BujoBot(bot).sendGenericError(ChatId(message), user.languageCode)
                }.unsafeRunSync()
            }
        }
    }

    fun createAction(bot: Bot, update: Update) =
        initNewActor(BujoBot(bot), update) { user, trackerUser, _, ctx ->
            CreateActionActor.yield(
                CreateActionState(ctx, trackerUser)
            ) {
                cause ?: with(state) {
                    ctx.client.createAction(trackerUser.id, ActionRequest(actionDescription, tags)).fold(
                        {
                            !sendLocalizedMessage(
                                state,
                                Lines::actionNotRegisteredMessage
                            )
                        },
                        { actionId ->
                            sendLocalizedMessage(
                                state, Lines::actionRegisteredMessage,
                                createdActionMarkup(state.trackerUser.language, actionId)
                            )
                        })
                }
                userActors.remove(BotUserId(user))
            }
        }

    fun addValue(
        callbackMessage: CallbackMessage,
        update: Update
    ) {
        initNewActor(callbackMessage.bot, update) { user, trackerUser, _, ctx ->
            AddValueActor.yield(
                AddValueState(
                    ctx,
                    trackerUser,
                    ActionId(UUID.fromString(callbackMessage.callBackData))
                )
            ) {
                with(state) {
                    ctx.client.addValue(trackerUser.id, actionId, Value(type, value, name)).fold(
                        {
                            !sendLocalizedMessage(state, Lines::addActionValueNotRegistered)
                        },
                        {
                            sendLocalizedMessage(state, Lines::addActionValueRegistered)
                        }
                    )
                }
                userActors.remove(BotUserId(user))
            }
        }
    }

    fun showSettings(bot: Bot, update: Update) {
        update.message?.let {
            bot.sendMessage(
                ChatId(it).value,
                BujoTalk.withLanguage(it.from?.languageCode).settingsMessage,
                replyMarkup = settingsMarkup(it.from?.languageCode)
            )
        }
    }

    fun createHabit(bot: Bot, update: Update) =
        initNewActor(BujoBot(bot), update) { user, trackerUser, _, ctx ->
            HabitActor.yield(
                CreateHabitState(ctx, trackerUser)
            ) {
                cause ?: with(state) {
                    val habitRequest =
                        HabitRequest(name, tags, numberOfRepetitions, period, quote, bad, startFrom, values)
                    ctx.client.createHabit(trackerUser.id, habitRequest).fold(
                        {
                            !sendLocalizedMessage(
                                this,
                                Lines::habitNotRegisteredMessage
                            )
                        },
                        { habitId ->
                            sendLocalizedMessage(
                                this,
                                Lines::habitRegisteredMessage,
                                habitCreatedMarkup(trackerUser.language, habitId)
                            )
                        })
                }
                userActors.remove(BotUserId(user))
            }
        }

    private fun initNewActor(
        bot: BujoBot,
        update: Update,
        init: (user: User, trackerUser: TrackerUser, message: Message, ctx: ActorContext) -> SendChannel<ActorMessage>?
    ) {
        (update.message ?: update.callbackQuery?.message)?.let { message ->
            (update.callbackQuery?.from ?: message.from)?.let { user: User ->
                launch {
                    val userId = BotUserId(user)
                    userActors[userId]?.close()
                    val actorContext = ActorContext(
                        ChatId(message),
                        BotUserId(user),
                        bot, this
                    )
                    actorContext.client.getUser(actorContext.userId)
                        .map { trackerUser ->
                            val initResult = init(
                                user,
                                trackerUser,
                                message,
                                actorContext
                            )
                            if (initResult != null) {
                                userActors[userId] = initResult
                            } else {
                                bot.sendGenericError(actorContext.chatId, user.languageCode)
                            }
                        }
                        .handleError {
                            bot.sendGenericError(actorContext.chatId, user.languageCode)
                        }.unsafeRunSync()

                }
            }
        }
    }

    fun showHabit(bot: Bot, query: CallbackQuery, habitId: UUID) {
        query.from.let { user ->
            IO.fx {
                val (trackerUser) = TrackerClient.getUser(BotUserId(user))
                val (habitInfo) = TrackerClient.getHabit(trackerUser.id, HabitId(habitId))
                val (habit, streakRow) = habitInfo
                with(BujoTalk.withLanguage(user.languageCode)) {
                    bot.sendMessage(
                        ChatId(query.message!!).value,
                        """
                            *$habitMessage:*
                            
                            *$nameMessage:* ${habit.name}   ${if (streakRow.currentStreak > BigDecimal.ONE) youAreOnStreakMessage.format(
                            streakRow.currentStreak
                        ) else ""}
                            *$tagsName:* ${habit.tags.joinToString(separator = " ") { "\uD83C\uDFF7${it.name}" }}
                            *$repetitionsMessage:* ${habit.numberOfRepetitions} / ${when (habit.period) {
                            Period.Week -> weekMessage
                            Period.Day -> dayMessage
                        }}
                            ${if (habit.quote?.isNotEmpty() == true) "*$quoteMessage*: ${habit.quote}" else ""}
                            ${habitValueTemplateNames(habit.values, user.languageCode)}
                        """.trimIndent(),
                        replyMarkup = habitMarkup(query.from.languageCode, habit),
                        parseMode = ParseMode.MARKDOWN
                    )
                }
            }.handleError {
                BujoBot(bot).sendGenericError(ChatId(query.message!!), user.languageCode)
            }.unsafeRunSync()
        }
    }

    private fun habitValueTemplateNames(values: List<ValueTemplate>, language: String?): String =
        values.map { template ->
            template.name ?: template.type.caption.get(BujoTalk.withLanguage(language))
        }.joinToString(separator = " ") { "🔎$it" }

    private fun showConfirmation(
        bot: Bot,
        query: CallbackQuery,
        performQuery: String,
        messageReference: KProperty1<Lines, String>,
        format: ((String) -> String) = { it -> it }
    ) {
        val languageCode = query.from.languageCode
        with(BujoTalk.withLanguage(languageCode)) {
            bot.sendMessage(
                ChatId(query.message!!).value,
                messageReference.get(this).let(format),
                replyMarkup = noYesMarkup(languageCode, performQuery)
            )
        }
    }

    fun showHabitDeleteConfirmation(bot: Bot, query: CallbackQuery, habitId: UUID) {
        query.from.let { user ->
            IO.fx {
                val (trackerUser) = TrackerClient.getUser(BotUserId(user))
                val (habitInfo) = TrackerClient.getHabit(trackerUser.id, HabitId(habitId))
                showConfirmation(
                    bot,
                    query,
                    "$CALLBACK_PERFORM_DELETE_HABIT: $habitId",
                    Lines::sureThatWantToDeleteTheHabit
                ) { it.format(habitInfo.habit.name) }
            }.handleError {
                BujoBot(bot).sendGenericError(ChatId(query.message!!), user.languageCode)
            }.unsafeRunSync()
        }

    }

    fun deleteHabit(bot: Bot, query: CallbackQuery, habitId: UUID) {
        val languageCode = query.from.languageCode
        with(BujoTalk.withLanguage(languageCode)) {
            IO.fx {
                val (user) = TrackerClient.getUser(BotUserId(query.from))
                val habitIdObject = HabitId(habitId)
                val (habitInfo) = TrackerClient.getHabit(user.id, habitIdObject)
                TrackerClient.deleteHabit(user.id, habitIdObject).toIO{ IllegalStateException(it.toString()) }.bind()
                bot.sendMessage(
                    ChatId(query.message!!).value,
                    habitDeletedMessage.format("**${habitInfo.habit.name}**"),
                    parseMode = ParseMode.MARKDOWN
                )
            }.handleError {
                bot.sendMessage(
                    ChatId(query.message!!).value,
                    habitNotDeletedMessage
                )
            }.unsafeRunSync()
        }
    }

    fun addFastHabitActionFromQuery(bot: Bot, update: Update, habitId: UUID) {
        val habitIdObject = HabitId(habitId)
        initNewActor(BujoBot(bot), update) { user, trackerUser, _, ctx ->
            IO.fx {
                val (habitInfo) = TrackerClient.getHabit(trackerUser.id, habitIdObject)
                val habit = habitInfo.habit
                AddFastValueListActor.yield(
                    AddFastValueListState(ctx, trackerUser, habit.values)
                ) {
                    cause ?: with(state) {
                        ctx.client.createHabitAction(
                            trackerUser.id,
                            habitIdObject,
                            HabitActionRequest(habit.name, habit.tags.map(Tag::toTagRequest), values)
                        ).fold(
                            {
                                !sendLocalizedMessage(
                                    state,
                                    Lines::actionNotRegisteredMessage
                                )
                            },
                            {
                                sendLocalizedMessage(state, Lines::actionRegisteredMessage)
                                showHabits(bot, update)
                            })
                    }
                    userActors.remove(BotUserId(user))
                }
            }.handleError { null }
                .unsafeRunSync()
        }
    }

//	fun editAction(message: CallbackMessage) {
//		launch {
//			val actorMessage = ActorMessage.Say(message.callBackData)
//			userActors[message.userId]?.close()
//			val channel = EditActionActor.yield(actorMessage, message.toContext(this))
//		}
//	}

}

sealed class BotMessage(
    val bot: BujoBot,
    private val chatId: ChatId,
    private val userId: BotUserId
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
        val streakCaption = if (habitsInfo.currentStreak > BigDecimal.ONE) " 🎯: ${habitsInfo.currentStreak}" else ""
        val habit = habitsInfo.habit

        val habitCaption = "${habit.name}$streakCaption"
        listOf(
            InlineKeyboardButton("◻️", callbackData = "$CALLBACK_ADD_FAST_HABIT_ACTION_BUTTON: ${habit.id?.value}"),
            InlineKeyboardButton(habitCaption, callbackData = "$CALLBACK_VIEW_HABIT: ${habit.id?.value}")
        )
    }

//"✅️"
private infix fun String.or(other: String) = if (Math.random() < 0.5) this else other
