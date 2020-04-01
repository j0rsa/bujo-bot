package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueTemplate
import com.j0rsa.bujo.telegram.bot.BujoLogic
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel

data class AddFastValueListState(
    override val ctx: ActorContext,
    override val trackerUser: TrackerUser,
    internal val templates: List<ValueTemplate>,
    internal val values: MutableList<Value> = mutableListOf()
) : ActorState(ctx, trackerUser)

@OptIn(ExperimentalCoroutinesApi::class)
object AddFastValueListActor : StateMachineActor<AddFastValueListState, List<Value>>(
    { values },
    mandatoryStep({
        state.subActor = initChain(state, state.templates.firstOrNull(), state.templates.drop(1))
        true
    }, {
        if (state.subActor != DummyChannel) {
            val result = CompletableDeferred<Boolean>()
            BujoLogic.handleSayActorMessage(message.text, state.subActor, result)
            result.await()
        }
        state.subActor == DummyChannel
    })
)

fun initChain(
    superState: AddFastValueListState,
    currentTemplate: ValueTemplate?,
    templates: List<ValueTemplate>
): SendChannel<ActorMessage> {
    return if (currentTemplate == null) DummyChannel else
        AddFastValueActor.yield(
            AddFastValueState(
                superState.ctx,
                superState.trackerUser,
                currentTemplate.type,
                currentTemplate.name
            )
        ) {
            superState.subActor = initChain(superState, templates.firstOrNull(), templates.drop(1))
            superState.values.add(result)
        }

}
