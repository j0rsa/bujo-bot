package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.monad.Client
import com.j0rsa.bujo.telegram.valueMarkup
import com.j0rsa.bujo.telegram.valueTypeMarkup
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddValueActorTest : ActorBotTest() {
	private val actionId = ActionId.randomValue()
	private val defaultName = "Mood"
	private val defaultType = ValueType.Mood
	private val defaultValue = "5"

	@Test
	fun testSuccessAddValue() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), actionId))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueInitMessage), replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueNameMessage))
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueValueMessage), replyMarkup = defaultValueMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueRegistered))
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun typeCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), actionId))
		actorChannel.send(skip(deferredFinished))

		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueInitMessage), replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))

		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	@Test
	fun valueCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), actionId))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(skip())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).addValue(user.id, actionId, defaultValue())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueInitMessage), replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueNameMessage))
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueValueMessage), replyMarkup = defaultValueMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueRegistered))

		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenSkipNameThenAddWithDefaultName() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), actionId))
		actorChannel.send(sayType())
		actorChannel.send(skip())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueInitMessage), replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueNameMessage))
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueValueMessage), replyMarkup = defaultValueMarkup())
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueRegistered))
		verify(client).addValue(user.id, actionId, defaultValue())
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	private fun sayType(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(defaultType.name, deferredFinished)

	private fun sayName(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say("Mood", deferredFinished)

	private fun sayValue(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say("5", deferredFinished)

	private fun defaultValueMarkup() = valueMarkup(defaultType)
	private fun defaultValue(type: ValueType = defaultType, name: String = defaultName) =
		Value(type, defaultValue, name)
}