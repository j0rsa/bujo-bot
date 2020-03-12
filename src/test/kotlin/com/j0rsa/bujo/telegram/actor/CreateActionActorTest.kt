package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.api.model.ActionId
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.createdAction
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateActionActorTest : ActorBotTest() {
	private val actionId = ActionId.randomValue()
	private val description = "description"
	private val tagsText = "tag1, tag2"

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
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::actionCreationInitMessage))
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::actionCreationTagsInput))
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::actionRegisteredMessage),
			replyMarkup = createdAction(actionId)
		)
		assertThat(deferredFinished.await()).isTrue()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnEmptyDescriptionThenCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
		}
		val deferredFinished = deferred()

		val actorChannel = CreateActionActor.yield(CreateActionState(actorContext(client)))
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
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

		val actorChannel = CreateActionActor.yield(CreateActionState(actorContext(client)))
		actorChannel.send(sayDescription())
		actorChannel.send(ActorMessage.Skip(deferredFinished))

		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
		assertThat(deferredFinished.await()).isFalse()
		actorChannel.close()
	}

	private fun sayTags(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(tagsText, deferredFinished)

	private fun sayDescription(deferredFinished: CompletableDeferred<Boolean> = deferred()) =
		ActorMessage.Say(description, deferredFinished)

	private fun defaultActionRequest(
		description: String = "description"
	): ActionRequest {
		return ActionRequest(
			description,
			listOf(TagRequest.fromString("tag1"), TagRequest.fromString("tag2"))
		)
	}
}