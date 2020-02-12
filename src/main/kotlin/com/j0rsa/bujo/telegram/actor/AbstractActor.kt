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
	class Cancel(d: CompletableDeferred<Boolean>) : ActorMessage(d)

	fun complete() = deferred.complete(true)
	fun completeExceptionally(exception: Throwable) = deferred.completeExceptionally(exception)
	fun unComplete() = deferred.complete(false)
}

interface Receiver {
	fun say(message: ActorMessage.Say): Receiver
	fun back(message: ActorMessage.Back): Receiver
	fun skip(message: ActorMessage.Skip): Receiver
	fun cancel(message: ActorMessage.Cancel): Receiver
}

object TerminatedReceiver : Receiver {
	override fun say(message: ActorMessage.Say): Receiver = complete(message)
	override fun back(message: ActorMessage.Back): Receiver = complete(message)
	override fun skip(message: ActorMessage.Skip): Receiver = complete(message)
	override fun cancel(message: ActorMessage.Cancel): Receiver = complete(message)

	private fun complete(message: ActorMessage): Receiver {
		message.completeExceptionally(IllegalStateException("We are done already!"))
		return this
	}
}