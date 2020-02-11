package com.j0rsa.bujo.telegram.monad

import com.j0rsa.bujo.telegram.BujoBot
import com.j0rsa.bujo.telegram.api.TrackerClient
import kotlinx.coroutines.CoroutineScope
import me.ivmg.telegram.Bot

/**
 * @author red
 * @since 08.02.20
 */

class Reader<D, out A>(val run: (D) -> A) {

    inline fun <B> map(crossinline fa: (A) -> B): Reader<D, B> = Reader { d ->
        fa(run(d))
    }

    inline fun <B> flatMap(crossinline fa: (A) -> Reader<D, B>): Reader<D, B> = Reader { d ->
        fa(run(d)).run(d)
    }

    companion object {
        fun <D, A> just(a: A): Reader<D, A> = Reader { a }
        fun <D> ask(): Reader<D, D> = Reader { it }
    }
}

data class ActorContext (
    val bot: BujoBot,
    val scope: CoroutineScope,
    val client: TrackerClient = TrackerClient
) {
    constructor(bot: Bot, scope: CoroutineScope): this(BujoBot(bot), scope)
}