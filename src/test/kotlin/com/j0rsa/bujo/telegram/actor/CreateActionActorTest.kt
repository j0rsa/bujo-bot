package com.j0rsa.bujo.telegram.actor

import arrow.core.Either
import assertk.assertThat
import com.j0rsa.bujo.telegram.Bot
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class CreateActionActorTest {
	private val chatId = 10L
	private val userId = 1L
	private val bot = mock<Bot>()
	private val user = User(UserId.randomValue(), 1L)
    private val actionId = ActionId.randomValue()

	@Test
	fun testActionCreation() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user
			on { createAction(user.id, defaultActionRequest()) } doReturn Either.Right(actionId)
		}
		val deferredFinished = CompletableDeferred<Boolean>()

		val actorChannel = CreateActionActor.yield(chatId, userId).run(ActorContext(bot, this, client))
		actorChannel.send(ActorMessage.Say("description", deferredFinished))
		actorChannel.send(ActorMessage.Say("tag1, tag2", deferredFinished))

		verify(client).getUser(userId)
		verify(client).createAction(user.id, defaultActionRequest())
		verify(bot).sendMessage(chatId, INIT_ACTION_TEXT)
		verify(bot).sendMessage(chatId, TAGS)
		verify(bot).sendMessage(chatId, ACTION_SUCCESS)
		assertThat(deferredFinished.await())
		actorChannel.close()
	}

	private fun defaultActionRequest(): ActionRequest {
		return ActionRequest(
			"description",
			listOf(TagRequest.fromString("tag1"), TagRequest.fromString("tag2"))
		)
	}
}