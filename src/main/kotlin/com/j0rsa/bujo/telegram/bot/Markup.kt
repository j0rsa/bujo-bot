package com.j0rsa.bujo.telegram.bot

import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import me.ivmg.telegram.entities.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author red
 * @since 14.03.20
 */

object Markup {
    fun permanentMarkup(language: String?) = with(
        BujoTalk.withLanguage(
            language
        )
    ) {
        KeyboardReplyMarkup(
            listOf(
                listOf(
                    KeyboardButton(showHabitsButton),
                    KeyboardButton(createHabitButton),
                    KeyboardButton(createActionButton)
                ),
                listOf(
                    KeyboardButton(settingsButton)
                )
            ),
            resizeKeyboard = true
        )
    }

    fun valueTypeMarkup(language: String): InlineKeyboardMarkup =
        InlineKeyboardMarkup(listOf(ValueType.values().flatMap {
            listOf(
                InlineKeyboardButton(
                    text = it.caption.get(BujoTalk.withLanguage(language)),
                    callbackData = "$CALLBACK_ACTOR_TEMPLATE:${it.name}"
                )
            )
        }))

    fun createdActionMarkup(language: String, actionId: ActionId): InlineKeyboardMarkup =
        with(BujoTalk.withLanguage(language)) {
            InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            text = viewActionButton,
                            callbackData = "$CALLBACK_VIEW_ACTION:${actionId.value}"
                        ),
                        addValueButton(actionId)
                    )
                )
            )
        }

    private fun addValueButton(actionId: ActionId) =
        InlineKeyboardButton(text = "Add value", callbackData = "$CALLBACK_ADD_VALUE:${actionId.value}")

    fun editActionMarkup(action: Action): InlineKeyboardMarkup = InlineKeyboardMarkup(
        (listOf(
            InlineKeyboardButton(text = "${action.description} ✏️", callbackData = "$TODO_TEMPLATE:1")
        ) + action.tags.mapIndexed { index, tag ->
            InlineKeyboardButton(text = "\uD83C\uDFF7 ${tag.name} ❌", callbackData = "$TODO_TEMPLATE:$index")
        } + listOf(InlineKeyboardButton(text = "+ 🏷️", callbackData = "$TODO_TEMPLATE:1"))
                + action.values.mapIndexed { index, value ->
            InlineKeyboardButton(text = "${value.name}: ${value.value}️ ❌", callbackData = "$TODO_TEMPLATE:$index")
        } + listOf(addValueButton(action.id!!))).chunked(1)
    )

    fun valueMarkup(type: ValueType) = when (type) {
        ValueType.Mood -> moodMarkup()
        ValueType.EndDate -> nowMarkup()
    }

    private fun moodMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton(text = "️\uD83D\uDE22", callbackData = "$CALLBACK_ACTOR_TEMPLATE:️1"),
                InlineKeyboardButton(text = "☹️", callbackData = "$CALLBACK_ACTOR_TEMPLATE:2"),
                InlineKeyboardButton(text = "\uD83D\uDE10", callbackData = "$CALLBACK_ACTOR_TEMPLATE:3"),
                InlineKeyboardButton(text = "\uD83D\uDE42", callbackData = "$CALLBACK_ACTOR_TEMPLATE:4"),
                InlineKeyboardButton(text = "\uD83D\uDE01", callbackData = "$CALLBACK_ACTOR_TEMPLATE:5")
            )
        )
    )

    fun periodMarkup(language: String): InlineKeyboardMarkup = with(
        BujoTalk.withLanguage(
            language
        )
    ) {
        InlineKeyboardMarkup(
            listOf(
                listOf(
                    InlineKeyboardButton(
                        text = periodDailyButton,
                        callbackData = "$CALLBACK_ACTOR_TEMPLATE:${Period.Day}"
                    ),
                    InlineKeyboardButton(
                        text = periodWeeklyButton,
                        callbackData = "$CALLBACK_ACTOR_TEMPLATE:${Period.Week}"
                    )
                )
            )
        )
    }

    fun noYesMarkup(language: String?, yesCallback: String): InlineKeyboardMarkup = with(
        BujoTalk.withLanguage(
            language
        )
    ) {
        InlineKeyboardMarkup(
            listOf(
                listOf(
                    InlineKeyboardButton(
                        text = noButton,
                        callbackData = CALLBACK_NO_BUTTON
                    ),
                    InlineKeyboardButton(
                        text = yesButton,
                        callbackData = yesCallback
                    )
                )
            )
        )
    }

    fun habitCreatedMarkup(language: String, habitId: HabitId): InlineKeyboardMarkup =
        with(BujoTalk.withLanguage(language)) {
            InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            text = viewHabitButton,
                            callbackData = "$CALLBACK_VIEW_HABIT:${habitId.value}"
                        )
                    )
                )
            )
        }

    fun newHabitMarkup(language: String?): InlineKeyboardMarkup =
        with(BujoTalk.withLanguage(language)) {
            InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            createHabitButton,
                            callbackData = CALLBACK_CREATE_HABIT_BUTTON
                        )
                    )
                )
            )
        }

    fun settingsMarkup(language: String?): InlineKeyboardMarkup =
        with(BujoTalk.withLanguage(language)) {
            InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            checkBackendButton,
                            callbackData = CALLBACK_SETTINGS_CHECK_BACKEND
                        )
                    )
                )
            )
        }

    private fun nowMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton(
                    text = "now: ${LocalDateTime.now()}",
                    callbackData = "$CALLBACK_ACTOR_TEMPLATE:️${LocalDateTime.now()}"
                )
            )
        )
    )

    fun habitMarkup(language: String?, habit: Habit): ReplyMarkup =
        with(BujoTalk.withLanguage(language)) {
            InlineKeyboardMarkup(
                tagButtons(habit.tags) +
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            addHabitActionButton,
                            callbackData = "$CALLBACK_ADD_FAST_HABIT_ACTION_BUTTON: ${habit.id?.value}"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton(
                            showActionsButton,
                            callbackData = "$CALLBACK_SHOW_ACTIONS_BUTTON:${habit.id?.value}:${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton(
                            deleteButton,
                            callbackData = "$CALLBACK_CONFIRM_DELETE_HABIT: ${habit.id?.value}"
                        )
                    )
                )
            )
        }

    private fun tagButtons(tags: List<Tag>): List<List<InlineKeyboardButton>> =
        tags.chunked(3).map { chunk ->
            chunk.map {
                InlineKeyboardButton(
                    "🏷${it.name}",
                    callbackData = "$CALLBACK_SHOW_HABITS_BY_TAG_ID_BUTTON: ${it.id.value}"
                )
            }
        }

    fun habitListMarkup(habits: List<HabitsInfo>) = InlineKeyboardMarkup(
        habits.toHabitsInlineKeys()
    )

    private fun List<HabitsInfo>.toHabitsInlineKeys(): List<List<InlineKeyboardButton>> =
        this.map { habitsInfo ->
            val streakCaption = if (habitsInfo.currentStreak > BigDecimal.ONE) " 🎯: ${habitsInfo.currentStreak}" else ""
            val habit = habitsInfo.habit

            val habitCaption = "${habit.name}$streakCaption"
            listOfNotNull(
                if (!habitsInfo.done)
                    InlineKeyboardButton("◻️", callbackData = "$CALLBACK_ADD_FAST_HABIT_ACTION_BUTTON: ${habit.id?.value}")
                else
                    InlineKeyboardButton("✅️", callbackData = "$CALLBACK_VIEW_HABIT: ${habit.id?.value}")
                ,
                InlineKeyboardButton(habitCaption, callbackData = "$CALLBACK_VIEW_HABIT: ${habit.id?.value}")
            )
        }

    fun datesPages(date: LocalDate, callbackPrefix: String, languageCode: String?): InlineKeyboardMarkup {
        val buttons = listOf(
            listOfNotNull(
                if(date != LocalDate.now()) InlineKeyboardButton(
                    BujoTalk.withLanguage(languageCode).toToday,
                    callbackData = "$callbackPrefix:${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                ) else null
            ),
            listOfNotNull(
                InlineKeyboardButton(
                    "<<",
                    callbackData = "$callbackPrefix:${date.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                ),
                if(date != LocalDate.now()) InlineKeyboardButton(
                    ">>",
                    callbackData = "$callbackPrefix:${date.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                ) else null
            )
        ).filterNot { it.isEmpty() }
        return InlineKeyboardMarkup(buttons)
    }

    private const val TODO_TEMPLATE = "todo"
}