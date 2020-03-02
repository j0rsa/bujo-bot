package com.j0rsa.bujo.telegram.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel

/**
 * @author red
 * @since 09.02.20
 */

interface Actor<T: ActorState> {
	fun yield(state: T): SendChannel<ActorMessage>
}

sealed class ActorMessage(private val deferred: CompletableDeferred<Boolean>) {
	data class Say(val text: String, val d: CompletableDeferred<Boolean> = CompletableDeferred()) : ActorMessage(d)
	data class Skip(val d: CompletableDeferred<Boolean> = CompletableDeferred()) : ActorMessage(d)

	fun complete() = deferred.complete(true)
	fun completeExceptionally(exception: Throwable) = deferred.completeExceptionally(exception)
	fun unComplete() = deferred.complete(false)
}

interface Receiver {
	fun say(message: ActorMessage.Say): Receiver
	fun skip(message: ActorMessage.Skip): Receiver
}