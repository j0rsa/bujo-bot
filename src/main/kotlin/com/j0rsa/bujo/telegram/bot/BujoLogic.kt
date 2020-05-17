package com.j0rsa.bujo.telegram.bot

import arrow.core.Either
import arrow.core.right
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.toIO
import arrow.fx.handleError
import com.j0rsa.bujo.telegram.Config
import com.j0rsa.bujo.telegram.WithLogger
import com.j0rsa.bujo.telegram.actor.*
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.actor.common.Localized
import com.j0rsa.bujo.telegram.api.TrackerClient
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.bot.BotMessage.CallbackMessage
import com.j0rsa.bujo.telegram.bot.Markup.createdActionMarkup
import com.j0rsa.bujo.telegram.bot.Markup.datesPages
import com.j0rsa.bujo.telegram.bot.Markup.habitCreatedMarkup
import com.j0rsa.bujo.telegram.bot.Markup.habitListMarkup
import com.j0rsa.bujo.telegram.bot.Markup.habitMarkup
import com.j0rsa.bujo.telegram.bot.Markup.newHabitMarkup
import com.j0rsa.bujo.telegram.bot.Markup.noYesMarkup
import com.j0rsa.bujo.telegram.bot.Markup.permanentMarkup
import com.j0rsa.bujo.telegram.bot.Markup.settingsMarkup
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Language
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.*
import org.http4k.core.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KProperty1

/**
 * @author red
 * @since 09.02.20
 */

@OptIn(ExperimentalCoroutinesApi::class)
object BujoLogic : CoroutineScope by CoroutineScope(Dispatchers.Default), WithLogger() {
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

    fun registerTelegramUser(bot: Bot, chatId: ChatId, user: User) {
                val (_, status) = TrackerClient.createUser(
                    CreateUserRequest(
                        telegramId = user.id,
                        firstName = user.firstName,
                        lastName = user.lastName ?: "",
                        language = user.languageCode ?: Config.app.defaultLanguage.toLowerCase()
                    )
                )
                val text = when (status) {
                    Status.CREATED -> BujoTalk.withLanguage(user.languageCode).welcome
                    Status.OK -> BujoTalk.withLanguage(user.languageCode).welcomeBack
                    else -> BujoTalk.withLanguage(user.languageCode).genericError
                }
                bot.sendMessage(
                    chatId.value,
                    text,
                    replyMarkup = permanentMarkup(user.languageCode)
                )
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
        channel: SendChannel<ActorMessage>,
        result: CompletableDeferred<Boolean> = CompletableDeferred()
    ) {
        launch {
            if (!channel.isClosedForSend) {
                channel.send(ActorMessage.Say(message, result))
            }
        }
    }

    fun handleUserActorSkipMessage(
        user: User,
        result: CompletableDeferred<Boolean> = CompletableDeferred()
    ) {
        launch {
            val userId = BotUserId(user)
            userActors[userId]?.let { actorChannel ->
                if (!actorChannel.isClosedForSend) {
                    actorChannel.send(ActorMessage.Skip(result))
                }
            }
        }
    }

    fun showHabits(bot: Bot, chatId: ChatId, user: User) {
        IO.fx {
            val (trackerUser) = TrackerClient.getUser(BotUserId(user.id))
            val (habits) = TrackerClient.getHabits(trackerUser.id)
            with(BujoTalk.withLanguage(user.languageCode)) {
                if (habits.isEmpty()) {
                    bot.sendMessage(
                        chatId.value,
                        text = noHabitsRegistered,
                        replyMarkup = newHabitMarkup(user.languageCode)
                    )
                } else {
                    bot.sendMessage(
                        chatId.value,
                        text = showHabitsMessage,
                        replyMarkup = habitListMarkup(habits)
                    )
                }
            }
        }.handleError {
            logger.error("IO error: $it")
            BujoBot(bot).sendGenericError(chatId, user.languageCode)
        }.unsafeRunSync()
    }

    fun createAction(bot: Bot, chatId: ChatId, user: User) =
        initNewActor(BujoBot(bot), chatId, user) { trackerUser, ctx ->
            CreateActionActor.yield(
                CreateActionState(ctx, trackerUser)
            ) {
                cause ?: ctx.client.createAction(trackerUser.id, result).fold(
                    {
                        logger.error("IO error: $it")
                        !sendLocalizedMessage(
                            Lines::actionNotRegisteredMessage
                        )
                    },
                    { actionId ->
                        sendLocalizedMessage(
                            Lines::actionRegisteredMessage,
                            createdActionMarkup(trackerUser.language, actionId)
                        )
                    })
            }
            userActors.remove(BotUserId(user))
        }

    fun addValue(
        callbackMessage: CallbackMessage,
        chatId: ChatId,
        user: User
    ) {
        initNewActor(callbackMessage.bot, chatId, user) { trackerUser, ctx ->
            val actionId = ActionId(UUID.fromString(callbackMessage.callBackData))
            AddValueActor.yield(
                AddValueState(
                    ctx,
                    trackerUser
                )
            ) {
                ctx.client.addValue(trackerUser.id, actionId, result).fold(
                    {
                        BujoLogic.logger.error("IO error: $it")
                        !sendLocalizedMessage(Lines::addActionValueNotRegistered)
                    },
                    {
                        sendLocalizedMessage(Lines::addActionValueRegistered)
                    }
                )
                userActors.remove(BotUserId(user))
            }
        }
    }

    fun showSettings(bot: Bot, chatId: ChatId, user: User) {
        bot.sendMessage(
            chatId.value,
            BujoTalk.withLanguage(user.languageCode).settingsMessage,
            replyMarkup = settingsMarkup(user.languageCode)
        )
    }

    fun createHabit(bot: Bot, chatId: ChatId, user: User) =
        initNewActor(BujoBot(bot), chatId, user) { trackerUser, ctx ->
            HabitActor.yield(
                CreateHabitState(ctx, trackerUser)
            ) {
                cause ?: ctx.client.createHabit(trackerUser.id, result).fold(
                    {
                        BujoLogic.logger.error("IO error: $it")
                        !sendLocalizedMessage(
                            Lines::habitNotRegisteredMessage
                        )
                    },
                    { habitId ->
                        sendLocalizedMessage(
                            Lines::habitRegisteredMessage,
                            habitCreatedMarkup(trackerUser.language, habitId)
                        )
                        showHabits(bot, chatId, user)
                    })
                userActors.remove(BotUserId(user))
            }
        }

    private fun initNewActor(
        bot: BujoBot,
        chatId: ChatId,
        user: User,
        init: (trackerUser: TrackerUser, ctx: ActorContext) -> SendChannel<ActorMessage>?
    ) {
        launch {
            val userId = BotUserId(user)
            userActors[userId]?.close()
            val actorContext = ActorContext(
                chatId,
                BotUserId(user),
                bot, this
            )
            actorContext.client.getUser(actorContext.userId)
                .map { trackerUser ->
                    val initResult = init(
                        trackerUser,
                        actorContext
                    )
                    if (initResult != null) {
                        userActors[userId] = initResult
                    }
                }
                .handleError {
                    logger.error("IO error: $it")
                    bot.sendGenericError(chatId, user.languageCode)
                }.unsafeRunSync()

        }
    }

    fun showHabit(bot: Bot, query: CallbackQuery, habitId: UUID) {
        query.from.let { user ->
            IO.fx {
                val (trackerUser) = TrackerClient.getUser(BotUserId(user))
                val (habitInfo) = TrackerClient.getHabit(trackerUser.id, HabitId(habitId))
                val (habit, _, streakRow) = habitInfo
                with(BujoTalk.withLanguage(user.languageCode)) {
                    bot.sendMessage(
                        ChatId(query.message!!).value,
                        """
                            *$habitMessage:*
                            
                            *$nameMessage:* ${habit.name}   
                            ${if (streakRow.currentStreak > BigDecimal.ONE) "*${youAreOnStreakMessage.format(streakRow.currentStreak.toInt())}* \uD83C\uDF89" else ""}
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
                logger.error("IO error: $it")
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
                logger.error("IO error: $it")
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
                TrackerClient.deleteHabit(user.id, habitIdObject).toIO { IllegalStateException(it.toString()) }.bind()
                bot.sendMessage(
                    ChatId(query.message!!).value,
                    habitDeletedMessage.format("**${habitInfo.habit.name}**"),
                    parseMode = ParseMode.MARKDOWN
                )
            }.handleError {
                logger.error("IO error: $it")
                bot.sendMessage(
                    ChatId(query.message!!).value,
                    habitNotDeletedMessage
                )
            }.unsafeRunSync()
        }
    }

    fun addFastHabitActionFromQuery(bot: Bot, chatId: ChatId, user: User, habitId: UUID) {
        val habitIdObject = HabitId(habitId)
        initNewActor(BujoBot(bot), chatId, user) { trackerUser, ctx ->
            IO.fx {
                val (habitInfo) = TrackerClient.getHabit(trackerUser.id, habitIdObject)
                val habit = habitInfo.habit
                if (habit.values.isEmpty()) {
                    createHabitActionWithMessage(ctx, trackerUser, habitIdObject, habit, bot, chatId, user, emptyList())
                    null
                } else
                    AddFastValueListActor.yield(
                        AddFastValueListState(ctx, trackerUser, habit.values)
                    ) {
                        cause ?: createHabitActionWithMessage(
                            ctx,
                            trackerUser,
                            habitIdObject,
                            habit,
                            bot,
                            chatId,
                            user,
                            result
                        )
                        userActors.remove(BotUserId(user))
                    }
            }.handleError {
                logger.error("IO error: $it")
                null
            }.unsafeRunSync()
        }
    }

    private fun createHabitActionWithMessage(
        ctx: ActorContext,
        trackerUser: TrackerUser,
        habitIdObject: HabitId,
        habit: Habit,
        bot: Bot,
        chatId: ChatId,
        user: User,
        result: List<Value>
    ) {
        val a = object : Localized {
            override fun context(): ActorContext = ctx
            override fun trackerUser(): TrackerUser = trackerUser
        }
        ctx.client.createHabitAction(
            trackerUser.id,
            habitIdObject,
            HabitActionRequest(habit.name, habit.tags.map(Tag::toTagRequest), result)
        ).fold(
            {
                !a.sendLocalizedMessage(
                    Lines::actionNotRegisteredMessage
                )
            },
            {
                a.sendLocalizedMessage(Lines::actionRegisteredMessage)
                showHabits(bot, chatId, user)
            })
    }

    fun showActions(bot: Bot, chatId: ChatId, user: User, habitId: UUID, date: LocalDate) {
        val languageCode = user.languageCode
        with(BujoTalk.withLanguage(languageCode)) {
            IO.fx {
                val trackerUser = !TrackerClient.getUser(BotUserId(user))
                val habitIdObject = HabitId(habitId)
                val habitInfo = !TrackerClient.getHabit(trackerUser.id, habitIdObject)
                when(val actions = TrackerClient.getHabitActions(trackerUser.id, habitIdObject, date)) {
                    is Either.Right -> bot.sendMessage(
                        chatId.value,
                        actionsForHabitMessage.format("**${habitInfo.habit.name}**")+ "\n" +
                        dateMessage.format(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))) + "\n" +
                        actionsToMessage(actions.b, languageCode),
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = datesPages(date, "$CALLBACK_SHOW_ACTIONS_BUTTON:$habitId:", languageCode)
                    ).right()
                    is Either.Left -> when(actions.a) {
                        is NotFound -> bot.sendMessage(
                            chatId.value,
                            noActionsThisDayMessage,
                            parseMode = ParseMode.MARKDOWN,
                            replyMarkup = datesPages(date, "$CALLBACK_SHOW_ACTIONS_BUTTON:$habitId:", languageCode)
                        ).right()
                        else -> actions
                    }
                }
            }.handleError {
                logger.error("IO error: $it")
                bot.sendMessage(
                    chatId.value,
                    habitNotDeletedMessage
                )
            }.unsafeRunSync()
        }
    }

    private fun actionsToMessage(actions: List<Action>, languageCode: String?): String {
        val lines = BujoTalk.withLanguage(languageCode)
        return actions.joinToString("\n") {
            it.values.joinToString(separator = ", ") { "${it.type.caption.get(lines)} ${it.value ?: ""}" }
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
    private val userId: BotUserId
) {
    class CallbackMessage(
        bot: BujoBot,
        userId: BotUserId,
        val callBackData: String
    ) : BotMessage(bot, userId)
}

class HandleActorMessage(
    val userId: BotUserId,
    val chatId: ChatId,
    val text: String
)
