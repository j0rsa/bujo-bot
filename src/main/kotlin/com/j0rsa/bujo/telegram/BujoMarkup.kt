package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.api.model.*
import me.ivmg.telegram.entities.InlineKeyboardButton
import me.ivmg.telegram.entities.InlineKeyboardMarkup
import java.time.LocalDateTime

/**
 * @author red
 * @since 14.03.20
 */

object BujoMarkup {
    fun valueTypeMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup(listOf(ValueType.values().flatMap {
        listOf(InlineKeyboardButton(text = it.name, callbackData = "$CALLBACK_ACTOR_TEMPLATE:${it.name}"))
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

    fun periodMarkup(language: String): InlineKeyboardMarkup = with(BujoTalk.withLanguage(language)) {
        InlineKeyboardMarkup(
            listOf(
                listOf(
                    InlineKeyboardButton(
                        text = periodDailyButton,
                        callbackData = "$CALLBACK_ACTOR_TEMPLATE:${Period.DAILY}"
                    ),
                    InlineKeyboardButton(
                        text = periodWeeklyButton,
                        callbackData = "$CALLBACK_ACTOR_TEMPLATE:${Period.WEEKLY}"
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

    private const val TODO_TEMPLATE = "todo"
}