package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.api.TrackerClient
import me.ivmg.telegram.Bot
import me.ivmg.telegram.entities.InlineKeyboardButton
import me.ivmg.telegram.entities.InlineKeyboardMarkup
import me.ivmg.telegram.entities.Update

/**
 * @author red
 * @since 09.02.20
 */

object BujoLogic {
    fun showMenu(bot: Bot, update: Update) {
        update.message?.let { message ->
            val text = if (TrackerClient.health()) {
                "😀"
            } else {
                "\uD83D\uDE30"
            }
            bot.sendMessage(
                message.chat.id,
                text
            )
        }
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

    fun createTelegramUser(bot: Bot, update: Update) {
        update.message?.let{ message ->
            message.from?.let {
                bot.sendMessage(
                    message.chat.id,
                    BujoTalk.getWelcomeMessage(BujoTalk.getLanguage(it.languageCode))
                )
            }
        }
    }
}