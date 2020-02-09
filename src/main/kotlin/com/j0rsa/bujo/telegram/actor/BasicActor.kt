package com.j0rsa.bujo.telegram.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor

/**
 * @author red
 * @since 09.02.20
 */

object BasicActor {
    sealed class BasicMessage{
        class Hey(val times: Int): BasicMessage()
        class Ho(val times: Int): BasicMessage()
        class LetsGo(val times: Int): BasicMessage()
        class Bye(val deferred: CompletableDeferred<String>) : BasicMessage()
    }
    @UseExperimental(ObsoleteCoroutinesApi::class)
    fun yield(scope: CoroutineScope) = with(scope) {
        actor<BasicMessage> {
            var finalMessage = ""
            for (message in channel) {
                when(message) {
                    is BasicMessage.Hey -> repeat(message.times) { finalMessage += "Hey " }
                    is BasicMessage.Ho -> repeat(message.times) { finalMessage += "Ho " }
                    is BasicMessage.LetsGo -> repeat(message.times) { finalMessage += "Let's go " }
                    is BasicMessage.Bye -> { message.deferred.complete(finalMessage) }
                }
            }
        }
    }
}