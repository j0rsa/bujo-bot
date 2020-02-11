package com.j0rsa.bujo.telegram.actor

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

    @Test
    fun testYield1() = runBlockingTest {
        val bot = mock<Bot>()
        val user = User(UserId.randomValue(), 1L)
        val client = mock<Client> {
            on { getUser(1L) } doReturn user
        }

        val actorChannel = CreateActionActor.yield(10L, 1L).run(ActorContext(bot, this, client))

        verify(client).getUser(1L)
        actorChannel.close()
    }
}