package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.model.User
import com.j0rsa.bujo.telegram.api.model.UserId
import com.j0rsa.bujo.telegram.bot.BotUserId
import com.j0rsa.bujo.telegram.bot.ChatId
import com.j0rsa.bujo.telegram.bot.TelegramBot
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlin.reflect.KProperty1

/**
 * @author red
 * @since 02.03.20
 */

open class ActorBotTest {
    protected val chatId = ChatId(10L)
    protected val userId = BotUserId(1L)
    protected val bot = mock<TelegramBot>()
    protected val user = User(UserId.randomValue(), 1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestCoroutineScope.actorContext(client: Client) =
        ActorContext(chatId, userId, bot, this, client)

    protected fun getLocalizedMessage(line: KProperty1<Lines, String>): String =
        line.get(BujoTalk.withLanguage(user.language))

    protected fun skip() = ActorMessage.Skip
}