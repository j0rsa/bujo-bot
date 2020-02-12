package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel

/**
 * @author red
 * @since 09.02.20
 */

interface Actor {
	fun yield(ctx: ActorContext): SendChannel<ActorMessage>
}

sealed class ActorMessage(val deferred: CompletableDeferred<Boolean>) {
	class Say(val text: String, d: CompletableDeferred<Boolean>) : ActorMessage(d)
	class Skip(d: CompletableDeferred<Boolean>) : ActorMessage(d)
	class Back(d: CompletableDeferred<Boolean>) : ActorMessage(d)

	fun complete() = deferred.complete(true)
	fun completeExceptionally(exception: Throwable) = deferred.completeExceptionally(exception)
	fun unComplete() = deferred.complete(false)
}