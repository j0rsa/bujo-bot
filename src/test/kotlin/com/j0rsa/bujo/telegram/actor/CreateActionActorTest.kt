package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.*
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.reflect.KProperty1

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class CreateActionActorTest {
	private val chatId = ChatId(10L)
	private val userId = BotUserId(1L)
	private val bot = mock<Bot>()
	private val user = User(UserId.randomValue(), 1L)
	private val actionId = ActionId.randomValue()
	private val description = "description"
	private val anotherDescription = "anotherDescription"
	private val tagsText = "tag1, tag2"
	private val tags = listOf(TagRequest("tag1"), TagRequest("tag2"))

	@Test
	fun testActionCreation() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = deferred()

		val state = CreateActionState(actorContext(client))
		val actorChannel = CreateActionActor.yield(state)
		actorChannel.send(sayDescription())
		actorChannel.send(sayTags(deferredFinished))

		verify(client).getUser(userId)
		verify(client).createAction(user.id, defaultActionRequest())
		verify(bot).sendMessage(chatId, getLocalizedMessage(user, Lines::actionCreationInitMessage))
		verify(bot).sendMessage(chatId, getLocalizedMessage(user, Lines::actionCreationTagsInput))
		verify(bot).sendMessage(chatId, getLocalizedMessage(user, Lines::actionRegisteredMessage))
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	private fun getLocalizedMessage(user: User, line: KProperty1<Lines, String>): String =
		line.get(BujoTalk.withLanguage(user.language))

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
		actorChannel.send(sayDescription())
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, CAN_NOT_BE_SKIPPED)
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
		actorChannel.send(sayDescription())
		actorChannel.send(back())
		actorChannel.send(skip(deferredFinished))

		verify(bot, times(2)).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, descriptionExistMessage(description))
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}


	private fun deferred() = CompletableDeferred<Boolean>()

	private fun sayTags(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(tagsText, deferredFinished)

	private fun sayDescription(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(description, deferredFinished)

	private fun skip(deferredFinished: CompletableDeferred<Boolean> = deferred()) = ActorMessage.Skip(deferredFinished)

	private fun TestCoroutineScope.actorContext(client: Client) =
		ActorContext(chatId, userId, bot, this, client)

	private fun defaultActionRequest(
		description: String = "description"
	): ActionRequest {
		return ActionRequest(
			description,
			listOf(TagRequest.fromString("tag1"), TagRequest.fromString("tag2"))
		)
	}
}