package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.BujoLogic.ActorCommand.*
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.dispatcher.message
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.extensions.filters.Filter
import okhttp3.logging.HttpLoggingInterceptor

/**
 * @author red
 * @since 02.02.20
 */

class App {
    fun run() {
        val bot = bot {
            token = Config.app.token
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                command("start") { bot, update -> BujoLogic.registerTelegramUser(bot, update) }
                command("habits") { bot, update -> BujoLogic.showHabits(bot, update) }
                command("skip") { _, update -> BujoLogic.handleActorMessage(update, Skip) }
                command("back") { _, update -> BujoLogic.handleActorMessage(update, Back) }
                command("cancel") { _, update -> BujoLogic.handleActorMessage(update, Cancel) }
                message(ShowHabitsButtonFilter) { bot, update -> BujoLogic.showHabits(bot, update) }
                message(CreateActionButtonFilter) { bot, update -> BujoLogic.createAction(bot, update) }
                message(Filter.Text and ShowHabitsButtonFilter.not() and CreateActionButtonFilter.not()) { _, update -> BujoLogic.handleActorMessage(update) }
            }
        }
        bot.startPolling()
    }

    object ShowHabitsButtonFilter : Filter {
        override fun Message.predicate(): Boolean =
            text == BujoTalk.withLanguage(from?.languageCode).showHabitsButton
    }

    object CreateActionButtonFilter : Filter {
        override fun Message.predicate(): Boolean =
            text == BujoTalk.withLanguage(from?.languageCode).createActionButton
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            App().run()
        }
    }
}