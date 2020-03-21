package com.j0rsa.bujo.telegram.bot

import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.ParseMode
import me.ivmg.telegram.entities.ReplyMarkup

interface TelegramBot {
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

class BujoBot(private val bot: Bot) : TelegramBot {
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
