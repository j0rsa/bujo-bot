package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.Bot
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description


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