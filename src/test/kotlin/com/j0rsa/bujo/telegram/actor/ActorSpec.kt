package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.api.model.UserId
import com.j0rsa.bujo.telegram.bot.BotUserId
import com.j0rsa.bujo.telegram.bot.ChatId
import com.j0rsa.bujo.telegram.bot.TelegramBot
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.mock
import io.kotest.core.config.Project
import io.kotest.core.spec.style.DslDrivenSpec
import io.kotest.core.spec.style.ShouldSpecDsl
import io.kotest.core.test.TestCaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KProperty1

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ActorSpec(body: ActorSpec.() -> Unit = {}) : DslDrivenSpec(), ShouldSpecDsl {
    val chatId = ChatId(10L)
    val userId = BotUserId(1L)
    val bot = mock<TelegramBot>()
    val user = TrackerUser(UserId.randomValue(), 1L)
    val skip = ActorMessage.Skip()

    fun CoroutineScope.actorContext(client: Client) =
        ActorContext(chatId, userId, bot, this, client)

    fun getLocalizedMessage(
        vararg lines: KProperty1<Lines, String>,
        format: String = lines.joinToString(separator = "\n") { "%s" }
    ): String =
        format.format(*
        lines.map { line -> line.get(BujoTalk.withLanguage(user.language)) }
            .toTypedArray()
        )

    override fun defaultConfig(): TestCaseConfig =
        defaultTestConfig ?: defaultTestCaseConfig() ?: Project.testCaseConfig()

    override val addTest = ::addRootTestCase

    init {
        body()
    }

    // need to overload this so that when doing "string" should haveLength(5) in a word spec, we don't
    // clash with the other should method
    // infix fun String.should(matcher: Matcher<String>) = TODO()
}