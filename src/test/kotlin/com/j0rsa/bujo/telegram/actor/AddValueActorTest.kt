package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.*
import com.j0rsa.bujo.telegram.actor.AddValueActor.defaultNameMessage
import com.j0rsa.bujo.telegram.actor.AddValueActor.notEmptyMessage
import com.j0rsa.bujo.telegram.actor.AddValueActor.typeExistMessage
import com.j0rsa.bujo.telegram.actor.AddValueActor.valueMessage
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class AddValueActorTest {
	private val chatId = ChatId(10L)
	private val userId = BotUserId(1L)
	private val bot = mock<Bot>()
	private val user = User(UserId.randomValue(), 1L)
	private val actionId = ActionId.randomValue()
	private val defaultName = "Mood"
	private val defaultType = ValueType.Mood
	private val defaultValue = "5"
	private val anotherName = "Another"
	private val anotherType = ValueType.EndDate

	@Test
	fun testSuccessAddValue() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).addValue(user.id, actionId, defaultValue())
		verify(bot).sendMessage(chatId, INIT_ADD_ACTION_VALUE, replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, defaultNameMessage())
		verify(bot).sendMessage(chatId, defaultValueMessage(), replyMarkup = defaultValueMarkup())
		verify(bot).valueAddedMessage(chatId, actionId)
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenGoBackOnTypeThenCancel() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(back(deferredFinished))

		verify(client).getUser(userId)
		verify(bot).sendMessage(chatId, VALUE_CANCELLED_TEXT)

		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun canGoBackToTypeAndChangeItThenAddValueWithAnotherType() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue(type = anotherType)) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(sayType())
		actorChannel.send(back())
		actorChannel.send(sayAnotherType())
		actorChannel.send(sayName())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).addValue(user.id, actionId, defaultValue(type = anotherType))
		verify(bot).sendMessage(chatId, typeExistMessage(defaultType), replyMarkup = valueTypeMarkup())
		verify(bot).valueAddedMessage(chatId, actionId)

		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}


	@Test
	fun canGoBackToTypeAndSkipItThenAddValueWithDefaultType() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(sayType())
		actorChannel.send(back())
		actorChannel.send(skip())
		actorChannel.send(sayName())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).addValue(user.id, actionId, defaultValue())
		verify(bot, never()).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
		verify(bot).valueAddedMessage(chatId, actionId)

		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun canGoBackToNameAndChangeItThenAddValueWithAnotherName() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { addValue(user.id, actionId, defaultValue(name = anotherName)) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(back())
		actorChannel.send(sayAnotherName())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).addValue(user.id, actionId, defaultValue(name = anotherName))
		verify(bot).sendMessage(chatId, defaultName.notEmptyMessage())
		verify(bot).valueAddedMessage(chatId, actionId)

		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun typeCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
		}
		val deferredFinished = deferred()

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(skip(deferredFinished))

		verify(bot).sendMessage(chatId, CAN_NOT_BE_SKIPPED)

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

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(skip())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).addValue(user.id, actionId, defaultValue())
		verify(bot).sendMessage(chatId, INIT_ADD_ACTION_VALUE, replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, defaultNameMessage())
		verify(bot).sendMessage(chatId, defaultValueMessage(), replyMarkup = defaultValueMarkup())
		verify(bot).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
		verify(bot).valueAddedMessage(chatId, actionId)

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

		val actorChannel = AddValueActor.yield(data(), actorContext(client))
		actorChannel.send(sayType())
		actorChannel.send(skip())
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(bot).sendMessage(chatId, INIT_ADD_ACTION_VALUE, replyMarkup = valueTypeMarkup())
		verify(bot).sendMessage(chatId, defaultNameMessage())
		verify(bot).sendMessage(chatId, defaultValueMessage(), replyMarkup = defaultValueMarkup())
		verify(client).addValue(user.id, actionId, defaultValue())
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	private fun sayType(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(defaultType.name, deferredFinished)

	private fun sayAnotherType(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(anotherType.name, deferredFinished)

	private fun sayName(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say("Mood", deferredFinished)

	private fun sayAnotherName(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(anotherName, deferredFinished)

	private fun sayValue(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say("5", deferredFinished)

	private fun back(deferredFinished: CompletableDeferred<Boolean> = deferred()) = ActorMessage.Back(deferredFinished)
	private fun skip(deferredFinished: CompletableDeferred<Boolean> = deferred()) = ActorMessage.Skip(deferredFinished)
	private fun cancel(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Cancel(deferredFinished)

	private fun defaultNameMessage() = defaultNameMessage(defaultType)
	private fun defaultValueMessage() = valueMessage(defaultName)

	private fun defaultValueMarkup() = valueMarkup(defaultType)
	private fun defaultValue(type: ValueType = defaultType, name: String = defaultName) =
		Value(type, defaultValue, name)

	private fun deferred() = CompletableDeferred<Boolean>()
	private fun TestCoroutineScope.actorContext(client: Client) =
		ActorContext(chatId, userId, bot, this, client)

	private fun data() = actionId.value.toString()
}