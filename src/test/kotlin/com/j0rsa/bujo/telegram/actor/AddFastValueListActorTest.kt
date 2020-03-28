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
import com.nhaarman.mockitokotlin2.verify
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AddFastValueListActorTest : ActorSpec({
    fun performActor(templates: List<ValueTemplate>, strings: List<String>): List<Value> = runBlocking {
        val client = mock<Client> {
            on { getUser(userId) } doReturn user.right().toIO()
        }

        val trackerUser = client.getUser(userId).unsafeRunSync()
        val state = AddFastValueListState(actorContext(client), trackerUser, templates)
        val actorChannel = AddFastValueListActor.yield(state)
        Thread.sleep(1000)
        strings.forEach {actorChannel.send(ActorMessage.Say(it)) }

        verify(client).getUser(userId)
        templates.forEach {
            val localizedCaption = it.name ?: it.type.caption.get(BujoTalk.withLanguage(state.trackerUser.language))
            verify(bot).sendMessage(
                chatId,
                getLocalizedMessage(Lines::whatIsYourMessage).format(localizedCaption),
                replyMarkup = Markup.valueMarkup(it.type)
            )
        }

        var values = emptyList<Value>()
        actorChannel.invokeOnClose {
            values = state.values
        }
        values
    }

    val templatesWithValues = listOf(
        ValueTemplate(ValueType.Mood) to "1",
        ValueTemplate(ValueType.Mood, name = "mood2") to "2",
        ValueTemplate(ValueType.EndDate, name = "mood2") to ZonedDateTime.now().toString()
    )

    fun templates(n: Int) = templatesWithValues.take(n).map { it.first }
    fun strings(n: Int) = templatesWithValues.take(n).map { it.second }
    fun values(n: Int) = templatesWithValues.take(n).map { Value(it.first.type, it.first.name, it.second) }

    "value addition" {
        table(
            headers("templates", "stings", "value"),
            *(1..templatesWithValues.size).map {
                row(templates(it), strings(it), values(it))
            }.toTypedArray()
        ).forAll { templates, strings, values ->
            performActor(templates, strings) shouldBe values
        }
    }
})