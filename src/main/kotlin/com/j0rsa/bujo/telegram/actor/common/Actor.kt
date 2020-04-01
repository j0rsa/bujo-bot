package com.j0rsa.bujo.telegram.actor.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel

/**
 * @author red
 * @since 09.02.20
 */

interface Actor<T : ActorState, OUT> {
    fun yield(
        state: T,
        onCompletionHandler: (ContextualResult<OUT>.() -> Unit)? = null
    ): SendChannel<ActorMessage>
}

sealed class ActorMessage(open val result: CompletableDeferred<Boolean>) {
    data class Say(
        val text: String,
        override val result: CompletableDeferred<Boolean> = CompletableDeferred()
    ) : ActorMessage(result)

    data class Skip(
        override val result: CompletableDeferred<Boolean> = CompletableDeferred()
    ) : ActorMessage(result)
}
