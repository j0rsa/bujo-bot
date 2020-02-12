package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import com.j0rsa.bujo.telegram.Bot
import com.j0rsa.bujo.telegram.actor.CreateActionActor.descriptionExistMessage
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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

	@Test
	fun testActionCreation() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Say(description, deferredFinished))
		actorChannel.send(ActorMessage.Say(tagsText, deferredFinished))

		verify(client).getUser(userId)
		verify(client).createAction(user.id, defaultActionRequest())
		verify(bot).sendMessage(chatId, INIT_ACTION_TEXT)
		verify(bot).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, ACTION_SUCCESS)
		assertThat(deferredFinished.await())
		actorChannel.close()
	}

	@Test
	fun whenSkipOnEmptyDescriptionThenNotSkippable() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, NOT_SKIPPABLE)
		assertThat(deferredFinished.isActive)
		actorChannel.close()
	}

	@Test
	fun whenBackOnDescriptionThenCancelled() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Back(deferredFinished))

		verify(bot).sendMessage(chatId, ACTION_CANCELLED_TEXT)
		assertThat(deferredFinished.await())
		actorChannel.close()
	}

	@Test
	fun whenBackOnTagsThenDescription() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Say(description, deferredFinished))
		actorChannel.send(ActorMessage.Back(deferredFinished))

		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		assertThat(deferredFinished.await())
		actorChannel.close()
	}

	@Test
	fun whenSkipOnDescriptionAfterFillingInAndComingBackFromTagThenCanBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Say(description, deferredFinished))
		actorChannel.send(ActorMessage.Back(deferredFinished))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot, times(2)).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		assertThat(deferredFinished.await())
		actorChannel.close()
	}

	@Test
	fun whenFillingInDescriptionAndComingBackFromTagThenUpdated() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest(anotherDescription)) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(actorContext(client))
		actorChannel.send(ActorMessage.Say(description, deferredFinished))
		actorChannel.send(ActorMessage.Back(deferredFinished))
		actorChannel.send(ActorMessage.Say(anotherDescription, deferredFinished))
		actorChannel.send(ActorMessage.Say(tagsText, deferredFinished))

		verify(client).getUser(userId)
		verify(client).createAction(user.id, defaultActionRequest(anotherDescription))
		verify(bot).sendMessage(chatId, INIT_ACTION_TEXT)
		verify(bot, times(2)).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		verify(bot).sendMessage(chatId, ACTION_SUCCESS)

		assertThat(deferredFinished.await())
		actorChannel.close()
	}

	private fun TestCoroutineScope.actorContext(client: Client) =
		ActorContext(chatId, userId, bot, this, client)

	private fun defaultActionRequest(description: String = "description"): ActionRequest {
		return ActionRequest(
			description,
			listOf(TagRequest.fromString("tag1"), TagRequest.fromString("tag2"))
		)
	}
}