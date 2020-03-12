package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.api.model.Action
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.Period
import com.j0rsa.bujo.telegram.api.model.ValueType
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.InlineKeyboardButton
import me.ivmg.telegram.entities.InlineKeyboardMarkup
import me.ivmg.telegram.entities.ParseMode
import me.ivmg.telegram.entities.ReplyMarkup
import java.time.LocalDateTime

interface Bot {
	fun sendMessage(
		chatId: ChatId,
		text: String,
		parseMode: ParseMode? = null,
		disableWebPagePreview: Boolean? = null,
		disableNotification: Boolean? = null,
		replyToMessageId: Long? = null,
		replyMarkup: ReplyMarkup? = null
	)

	fun editMessageText(
		chatId: Long? = null,
		messageId: Long? = null,
		inlineMessageId: String? = null,
		text: String,
		parseMode: ParseMode? = null,
		disableWebPagePreview: Boolean? = null,
		replyMarkup: ReplyMarkup? = null
	)
}

class BujoBot(private val bot: Bot) : com.j0rsa.bujo.telegram.Bot {
	override fun sendMessage(
		chatId: ChatId,
		text: String,
		parseMode: ParseMode?,
		disableWebPagePreview: Boolean?,
		disableNotification: Boolean?,
		replyToMessageId: Long?,
		replyMarkup: ReplyMarkup?
	) {
		bot.sendMessage(
			chatId.value,
			text,
			parseMode,
			disableWebPagePreview,
			disableNotification,
			replyToMessageId,
			replyMarkup
		)
	}

	override fun editMessageText(
		chatId: Long?,
		messageId: Long?,
		inlineMessageId: String?,
		text: String,
		parseMode: ParseMode?,
		disableWebPagePreview: Boolean?,
		replyMarkup: ReplyMarkup?
	) {
		bot.editMessageText(
			chatId,
			messageId,
			inlineMessageId,
			text,
			parseMode,
			disableWebPagePreview,
			replyMarkup
		)
	}
}

fun valueTypeMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup(listOf(ValueType.values().flatMap {
	listOf(InlineKeyboardButton(text = it.name, callbackData = "$CALLBACK_ACTOR_TEMPLATE:${it.name}"))
}))

fun createdAction(actionId: ActionId): InlineKeyboardMarkup = InlineKeyboardMarkup(
	listOf(
		listOf(
			InlineKeyboardButton(text = "View action", callbackData = "$CALLBACK_VIEW_ACTION:${actionId.value}"),
			addValueButton(actionId)
		)
	)
)

private fun addValueButton(actionId: ActionId) =
	InlineKeyboardButton(text = "Add value", callbackData = "$CALLBACK_ADD_VALUE:${actionId.value}")

fun editAction(action: Action): InlineKeyboardMarkup = InlineKeyboardMarkup(
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
			InlineKeyboardButton(text = "️\uD83D\uDE22", callbackData = "$CALLBACK_ACTOR_TEMPLATE:️\uD83D\uDE22"),
			InlineKeyboardButton(text = "☹️", callbackData = "$CALLBACK_ACTOR_TEMPLATE:☹️"),
			InlineKeyboardButton(text = "\uD83D\uDE10", callbackData = "$CALLBACK_ACTOR_TEMPLATE:\uD83D\uDE10"),
			InlineKeyboardButton(text = "\uD83D\uDE42", callbackData = "$CALLBACK_ACTOR_TEMPLATE:\uD83D\uDE42"),
			InlineKeyboardButton(text = "\uD83D\uDE01", callbackData = "$CALLBACK_ACTOR_TEMPLATE:\uD83D\uDE01")
		)
	)
)

fun periodMarkup(language: String): InlineKeyboardMarkup = with(BujoTalk.withLanguage(language)) {
	InlineKeyboardMarkup(
		listOf(
			listOf(
				InlineKeyboardButton(text = periodDaily, callbackData = "$CALLBACK_ACTOR_TEMPLATE:${Period.DAILY}"),
				InlineKeyboardButton(text = periodWeekly, callbackData = "$CALLBACK_ACTOR_TEMPLATE:${Period.WEEKLY}")
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

const val TODO_TEMPLATE = "todo"