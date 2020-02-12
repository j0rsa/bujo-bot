package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.Bot
import com.j0rsa.bujo.telegram.actor.CreateActionActor.descriptionExistMessage
import com.j0rsa.bujo.telegram.actor.CreateActionActor.tagsExistMessage
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class CreateActionActorTest {
	private val chatId = 10L
	private val userId = 1L
	private val bot = mock<Bot>()
	private val user = User(UserId.randomValue(), 1L)
	private val actionId = ActionId.randomValue()
	private val description = "description"
	private val anotherDescription = "anotherDescription"
	private val tagsText = "tag1, tag2"
	private val tags = listOf(TagRequest("tag1"), TagRequest("tag2"))
	private val value = "5"

	@Test
	fun testActionCreation() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(sayTags(deferred()))
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).createAction(user.id, defaultActionRequest())
		verify(bot).sendMessage(chatId, INIT_ACTION_TEXT)
		verify(bot).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, VALUES)
		verify(bot).sendMessage(chatId, ACTION_SUCCESS)
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnEmptyDescriptionThenCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnEmptyTagsThenCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnNotEmptyTagsThenCanBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(sayTags(deferred()))
		actorChannel.send(ActorMessage.Back(deferred()))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, tagsExistMessage(tags))
		verify(bot, never()).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnEmptyValuesThenCanBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest(values = emptyList())) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(sayTags(deferred()))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(client).createAction(user.id, defaultActionRequest(values = emptyList()))
		verify(bot).sendMessage(chatId, INIT_ACTION_TEXT)
		verify(bot).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, VALUES)
		verify(bot, never()).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
		verify(bot).sendMessage(chatId, ACTION_SUCCESS)
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenBackOnDescriptionThenCancelled() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Back(deferredFinished))

		verify(bot).sendMessage(chatId, ACTION_CANCELLED_TEXT)
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenBackOnTagsThenDescription() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(ActorMessage.Back(deferredFinished))

		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnNotEmptyDescriptionThenCanBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(ActorMessage.Back(deferred()))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot, times(2)).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	@Test
	fun whenNotEmptyDescriptionAndComingBackFromTagThenUpdated() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest(anotherDescription)) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(ActorMessage.Back(deferred()))
		actorChannel.send(ActorMessage.Say(anotherDescription, deferred()))
		actorChannel.send(sayTags(deferred()))
		actorChannel.send(sayValue(deferredFinished))

		verify(client).getUser(userId)
		verify(client).createAction(user.id, defaultActionRequest(anotherDescription))
		verify(bot).sendMessage(chatId, INIT_ACTION_TEXT)
		verify(bot, times(2)).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		verify(bot).sendMessage(chatId, ACTION_SUCCESS)

		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenCancel() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(sayDescription(deferred()))
		actorChannel.send(sayTags(deferred()))
		actorChannel.send(ActorMessage.Cancel(deferredFinished))

		verify(bot).sendMessage(chatId, ACTION_CANCELLED_TEXT)
		verify(client, never()).createAction(eq(user.id), any())
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	private fun deferred() = CompletableDeferred<Boolean>()

	private fun sayTags(deferredFinished: CompletableDeferred<Boolean>) =
		ActorMessage.Say(tagsText, deferredFinished)

	private fun sayDescription(deferredFinished: CompletableDeferred<Boolean>) =
		ActorMessage.Say(description, deferredFinished)

	private fun sayValue(deferredFinished: CompletableDeferred<Boolean>) =
		ActorMessage.Say(value, deferredFinished)

	private fun TestCoroutineScope.actorContext(client: Client) =
		ActorContext(chatId, userId, bot, this, client)

	private fun defaultActionRequest(
		description: String = "description",
		values: List<Value> = listOf(Value(ValueType.Mood, "5", "mood"))
	): ActionRequest {
		return ActionRequest(
			description,
			listOf(TagRequest.fromString("tag1"), TagRequest.fromString("tag2")),
			values
		)
	}
}