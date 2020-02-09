package com.j0rsa.bujo.telegram.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import me.ivmg.telegram.Bot

/**
 * @author red
 * @since 09.02.20
 */

interface Actor{
    fun yield(scope: CoroutineScope, bot: Bot, chatId: Long, userId: Long): SendChannel<ActorMessage>
}

sealed class ActorMessage {
    class Say(val text: String, val deferred: CompletableDeferred<Boolean>) : ActorMessage()
}