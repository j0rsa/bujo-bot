package com.j0rsa.bujo.telegram.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import me.ivmg.telegram.Bot
import java.lang.IllegalStateException

/**
 * @author red
 * @since 09.02.20
 */

object HabitActor: Actor {
    @UseExperimental(ObsoleteCoroutinesApi::class)
    override fun yield(scope: CoroutineScope, bot: Bot, chatId: Long, userId: Long) = with(scope) {
        actor<ActorMessage> {
            var habitName = ""
            var habitDuration = ""

            //INIT ACTOR
            //FINISH INIT
            var state: CreateHabitState =  CreateHabitState.HabitName

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
                                message.deferred.completeExceptionally(IllegalStateException("We are done already!"))
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