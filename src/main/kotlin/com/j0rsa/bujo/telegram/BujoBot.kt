package com.j0rsa.bujo.telegram

import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.ParseMode
import me.ivmg.telegram.entities.ReplyMarkup

interface Bot {
    fun sendMessage(
        chatId: Long,
        text: String,
        parseMode: ParseMode? = null,
        disableWebPagePreview: Boolean? = null,
        disableNotification: Boolean? = null,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null
    )
}

inline class BujoBot(val bot: Bot) : com.j0rsa.bujo.telegram.Bot {
    override fun sendMessage(
        chatId: Long,
        text: String,
        parseMode: ParseMode?,
        disableWebPagePreview: Boolean?,
        disableNotification: Boolean?,
        replyToMessageId: Long?,
        replyMarkup: ReplyMarkup?
    ) {
        bot.sendMessage(
            chatId,
            text,
            parseMode,
            disableWebPagePreview,
            disableNotification,
            replyToMessageId,
            replyMarkup
        )
    }
}