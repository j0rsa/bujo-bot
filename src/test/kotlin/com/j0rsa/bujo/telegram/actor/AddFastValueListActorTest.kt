package com.j0rsa.bujo.telegram.actor

import arrow.core.right
import arrow.fx.extensions.toIO
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueTemplate
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.bot.Markup
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AddFastValueListActorTest : ActorSpec({
    fun performActor(templates: List<ValueTemplate>, strings: List<String>): List<Value> = runBlocking {
        val client = mock<Client> {
            on { getUser(userId) } doReturn user.right().toIO()
        }
        val trackerUser = client.getUser(userId).unsafeRunSync()
        val state = AddFastValueListState(actorContext(client), trackerUser, templates)
        var actorResult = emptyList<Value>()
        val actorChannel = AddFastValueListActor.yield(state) {
            actorResult = result
        }
        strings.forEach {
            val result = CompletableDeferred<Boolean>()
            actorChannel.send(ActorMessage.Say(it, result))
            result.await()
        }

        verify(client).getUser(userId)
        templates.forEach {
            val localizedCaption = it.name ?: it.type.caption.get(BujoTalk.withLanguage(state.trackerUser.language))
            verify(bot).sendMessage(
                chatId,
                getLocalizedMessage(Lines::whatIsYourMessage).format(localizedCaption),
                replyMarkup = Markup.valueMarkup(it.type)
            )
        }
        actorResult
    }

    val templatesWithValues = listOf(
        ValueTemplate(ValueType.Mood) to "1",
        ValueTemplate(ValueType.Mood, name = "mood2") to "2",
        ValueTemplate(ValueType.Mood, name = "mood3") to "3",
        ValueTemplate(ValueType.Mood, name = "mood4") to "4"
    )

    fun templates(n: Int) = templatesWithValues.take(n).map { it.first }
    fun strings(n: Int) = templatesWithValues.take(n).map { it.second }
    fun values(n: Int) = templatesWithValues.take(n).map { Value(it.first.type, it.second, it.first.name) }

    "value addition" {
        (1..templatesWithValues.size).forEach {
            should("handle $it value${if(it!=1) "s" else "" }") {
                Mockito.clearInvocations(bot)
                performActor(templates(it), strings(it)) shouldBe values(it)
            }
        }
    }
})