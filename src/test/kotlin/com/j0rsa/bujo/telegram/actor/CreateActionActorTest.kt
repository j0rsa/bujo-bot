package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import arrow.core.right
import arrow.fx.extensions.toIO
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.bot.ActionId
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
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
			on { getUser(userId) } doReturn user.right().toIO()
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val state = CreateActionState(actorContext(client), trackerUser)
		val actorChannel = CreateActionActor.yield(state)
		actorChannel.send(sayDescription())
		actorChannel.send(sayTags())

		verify(client).getUser(userId)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::actionCreationInitMessage, Lines::actionCreationDescriptionInput, format = "%s\n%s"))
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::actionCreationTagsInput))
		assertThat(actorChannel.isClosedForSend).isTrue()
	}

	@Test
	fun whenSkipOnEmptyDescriptionThenCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user.right().toIO()
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val actorChannel = CreateActionActor.yield(CreateActionState(actorContext(client), trackerUser))
		actorChannel.send(ActorMessage.Skip())

		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
		assertThat(actorChannel.isClosedForSend).isFalse()
		actorChannel.close()
	}

	@Test
	fun whenSkipOnEmptyTagsThenCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user.right().toIO()
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val actorChannel = CreateActionActor.yield(CreateActionState(actorContext(client), trackerUser))
		actorChannel.send(sayDescription())
		actorChannel.send(ActorMessage.Skip())

		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
		assertThat(actorChannel.isClosedForSend).isFalse()
		actorChannel.close()
	}

	private fun sayTags() = ActorMessage.Say(tagsText)
	private fun sayDescription() = ActorMessage.Say(description)

	private fun defaultActionRequest(
		description: String = "description"
	): ActionRequest {
		return ActionRequest(
			description,
			listOf(TagRequest.fromString("tag1"), TagRequest.fromString("tag2"))
		)
	}
}