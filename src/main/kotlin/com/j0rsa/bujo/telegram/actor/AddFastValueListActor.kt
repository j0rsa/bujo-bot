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
    internal val values: MutableList<Value> = mutableListOf(),
    internal val iterator: Iterator<ValueTemplate> = templates.iterator(),
    internal var actorSuccessfullyFinished: CompletableDeferred<Boolean> = CompletableDeferred()
) : ActorState(ctx, trackerUser)

@OptIn(ExperimentalCoroutinesApi::class)
object AddFastValueListActor : StateMachineActor<AddFastValueListState, List<Value>>(
    { values },
    mandatoryStep({
        state.subActor = initValueActor(state, state.iterator.next())
        true
    }, {
        val result = CompletableDeferred<Boolean>()
        BujoLogic.handleSayActorMessage(message.text, state.subActor, result)
        result.await()
        val actorResult = state.actorSuccessfullyFinished.await()
        if (actorResult) {
            if (state.iterator.hasNext()) {
                state.subActor = initValueActor(state, state.iterator.next())
                false
            } else {
                true
            }
        } else {
            false
        }
    })
)

fun initValueActor(
    superState: AddFastValueListState,
    currentTemplate: ValueTemplate
): SendChannel<ActorMessage> {
    superState.actorSuccessfullyFinished = CompletableDeferred()
    return AddFastValueActor.yield(
        AddFastValueState(
            superState.ctx,
            superState.trackerUser,
            currentTemplate.type,
            currentTemplate.name
        )
    ) {
        superState.values.add(result)
        superState.actorSuccessfullyFinished.complete(true)
    }
}