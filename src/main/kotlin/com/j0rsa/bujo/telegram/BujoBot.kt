package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.actor.ACTION_SUCCESS
import com.j0rsa.bujo.telegram.actor.ADD_VALUE_ACTION_SUCCESS
import com.j0rsa.bujo.telegram.api.model.Action
import com.j0rsa.bujo.telegram.api.model.ActionId
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

	fun actionCreatedMessage(chatId: ChatId, actionId: ActionId)
	fun editMessageWithValueType(chatId: ChatId, messageId: Long?, text: String)
	fun valueAddedMessage(chatId: ChatId, actionId: ActionId)
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

	override fun editMessageWithValueType(chatId: ChatId, messageId: Long?, text: String) {
		bot.editMessageText(chatId.value, messageId, text = text, replyMarkup = valueTypeMarkup())
	}

	override fun valueAddedMessage(chatId: ChatId, actionId: ActionId) {
		bot.sendMessage(chatId = chatId.value, text = ADD_VALUE_ACTION_SUCCESS, replyMarkup = createdAction(actionId))
	}

	override fun actionCreatedMessage(chatId: ChatId, actionId: ActionId) {
		bot.sendMessage(chatId = chatId.value, text = ACTION_SUCCESS, replyMarkup = createdAction(actionId))
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
	) + action.tags.map {
		InlineKeyboardButton(text = "\uD83C\uDFF7 ${it.name} ❌", callbackData = "$TODO_TEMPLATE:1")
	} + listOf(InlineKeyboardButton(text = "+ 🏷️", callbackData = "$TODO_TEMPLATE:1"))
			+ action.values.map {
		InlineKeyboardButton(text = "${it.name}: ${it.value}️ ❌", callbackData = "$TODO_TEMPLATE:1")
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

const val CALLBACK_ACTOR_TEMPLATE = "actor"
const val CALLBACK_ADD_VALUE = "addActionValue"
const val CALLBACK_VIEW_ACTION = "viewAction"
const val TODO_TEMPLATE = "todo"