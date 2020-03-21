package com.j0rsa.bujo.telegram.actor.common

import kotlinx.coroutines.channels.SendChannel

/**
 * @author red
 * @since 09.02.20
 */

interface Actor<T: ActorState> {
	fun yield(
		state: T,
		onCompletionHandler: (StateWithLocalization<T>.() -> Unit)? = null
	): SendChannel<ActorMessage>
}

sealed class ActorMessage {
	data class Say(val text: String) : ActorMessage()
	object Skip : ActorMessage()
}
