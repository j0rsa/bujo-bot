package com.j0rsa.bujo.telegram.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.lang.IllegalStateException

/**
 * @author red
 * @since 09.02.20
 */

object HabitActor {
    @UseExperimental(ObsoleteCoroutinesApi::class)
    fun yield(scope: CoroutineScope) = with(scope) {
        actor<CreateHabitMessage> {
            var habitName = ""
            var habitDuration = ""

            //INIT ACTOR
            //FINISH INIT
            var state: CreateHabitState =  CreateHabitState.HabitName

            for (message in channel) {
                when (message) {
                    is CreateHabitMessage.Say ->
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

sealed class CreateHabitMessage {
    class Say(val text: String, val deferred: CompletableDeferred<Boolean>) : CreateHabitMessage()
}

sealed class CreateHabitState {
    object HabitName : CreateHabitState()
    object HabitDuration : CreateHabitState()
    object Terminated : CreateHabitState()
}