package com.j0rsa.bujo.telegram

import com.j0rsa.bujo.telegram.api.TrackerClient
import com.j0rsa.bujo.telegram.monad.Reader
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.text

/**
 * @author red
 * @since 02.02.20
 */

class App {
    fun run() {
        bot {
            token = Config.app.token
            dispatch {
                text { bot, update ->

                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            App().run()
        }
    }
}