package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Reader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor

/**
 * @author red
 * @since 09.02.20
 */

object HabitActor : Actor {
    @UseExperimental(ObsoleteCoroutinesApi::class)
    override fun yield(chatId: Long, userId: Long) =
        Reader.ask<ActorContext>().map { yield2(chatId, userId, it) }

    private fun yield2(
        chatId: Long,
        userId: Long,
        ctx: ActorContext
    ) = with(ctx.scope) {
        actor<ActorMessage> {
            var habitName = ""
            var habitDuration = ""

            //INIT ACTOR
            //FINISH INIT
            var state: CreateHabitState = CreateHabitState.HabitName

            for (message in channel) {
                when (message) {
                    is ActorMessage.Say ->
                        when (state) {
                            CreateHabitState.HabitName -> {
                                habitName = message.text
                                state = CreateHabitState.HabitDuration
                                //send message: Enter duration

                                // flow is not finished
                                message.deferred.complete(false)
                            }
                            CreateHabitState.HabitDuration -> {
                                //received last item
                                //call API
                                state = CreateHabitState.Terminated
                                message.deferred.complete(true)
                            }
                            CreateHabitState.Terminated -> {
                                message.deferred.completeExceptionally(java.lang.IllegalStateException("We are done already!"))
                            }
                        }
                }
            }
        }
    }


}

sealed class CreateHabitState {
    object HabitName : CreateHabitState()
    object HabitDuration : CreateHabitState()
    object Terminated : CreateHabitState()
}