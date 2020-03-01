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
	class Say(val text: String, d: CompletableDeferred<Boolean> = CompletableDeferred()) : ActorMessage(d)
	class Skip(d: CompletableDeferred<Boolean> = CompletableDeferred()) : ActorMessage(d)

	fun complete() = deferred.complete(true)
	fun completeExceptionally(exception: Throwable) = deferred.completeExceptionally(exception)
	fun unComplete() = deferred.complete(false)
}

interface Receiver {
	fun say(message: ActorMessage.Say): Receiver
	fun skip(message: ActorMessage.Skip): Receiver
}