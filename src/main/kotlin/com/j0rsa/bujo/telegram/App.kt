package com.j0rsa.bujo.telegram

import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.extensions.filters.Filter

/**
 * @author red
 * @since 02.02.20
 */

class App {
    fun run() {
        val bot = bot {
            token = Config.app.token
            dispatch {
                command("start") { bot, udate ->
                    BujoLogic.createTelegramUser(bot, udate)

                }
//                message(NotCommand) { bot, update -> BujoLogic.handleMessageInContext(bot, update) }
            }
        }
        bot.startPolling()
    }

    object NotCommand : Filter {
        override fun Message.predicate(): Boolean {
            return text != null && !text!!.startsWith("/")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            App().run()
        }
    }
}