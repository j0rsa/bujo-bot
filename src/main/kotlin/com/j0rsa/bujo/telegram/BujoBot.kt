package com.j0rsa.bujo.telegram

import com.google.gson.Gson
import com.j0rsa.bujo.telegram.actor.ACTION_SUCCESS
import com.j0rsa.bujo.telegram.actor.ADD_VALUE_ACTION_SUCCESS
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.ValueId
import com.j0rsa.bujo.telegram.api.model.ValueType
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.InlineKeyboardButton
import me.ivmg.telegram.entities.InlineKeyboardMarkup
import me.ivmg.telegram.entities.ParseMode
import me.ivmg.telegram.entities.ReplyMarkup

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
	fun valueAddedMessage(chatId: ChatId, actionId: ActionId, valueId: ValueId)
}

class BujoBot(val bot: Bot) : com.j0rsa.bujo.telegram.Bot {
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

	override fun valueAddedMessage(chatId: ChatId, actionId: ActionId, valueId: ValueId) {
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
			InlineKeyboardButton(text = "View action", callbackData = "viewAction:${actionId.value}"),
			InlineKeyboardButton(text = "Add value", callbackData = "$CALLBACK_ADD_VALUE:${actionId.value}")
		)
	)
)

fun valueMarkup(type: ValueType) = when (type) {
	ValueType.Mood -> moodMarkup()
	ValueType.EndDate -> null
}

private fun moodMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup(
	listOf(
		listOf(
			InlineKeyboardButton(text = "1", callbackData = "$CALLBACK_ACTOR_TEMPLATE:1"),
			InlineKeyboardButton(text = "2", callbackData = "$CALLBACK_ACTOR_TEMPLATE:2"),
			InlineKeyboardButton(text = "3", callbackData = "$CALLBACK_ACTOR_TEMPLATE:3"),
			InlineKeyboardButton(text = "4", callbackData = "$CALLBACK_ACTOR_TEMPLATE:4"),
			InlineKeyboardButton(text = "5", callbackData = "$CALLBACK_ACTOR_TEMPLATE:5")
		)
	)
)

const val CALLBACK_ACTOR_TEMPLATE = "actor"
const val CALLBACK_ADD_VALUE = "addActionValue"