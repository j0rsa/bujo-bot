package com.j0rsa.bujo.telegram.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * @author red
 * @since 09.02.20
 */

internal class BasicActorTest {

    @Test
    fun yield() {
        runBlocking {
            val actorChannel = BasicActor.yield(this)
            actorChannel.send(BasicActor.BasicMessage.Hey(1))
            actorChannel.send(BasicActor.BasicMessage.Ho(2))
            actorChannel.send(BasicActor.BasicMessage.LetsGo(1))
            val deferred = CompletableDeferred<String>()
            actorChannel.send(BasicActor.BasicMessage.Bye(deferred))

            println(deferred.await())

            actorChannel.close()
        }
    }
}