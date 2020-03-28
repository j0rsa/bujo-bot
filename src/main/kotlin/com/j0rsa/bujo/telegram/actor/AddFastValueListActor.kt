package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueTemplate
import com.j0rsa.bujo.telegram.bot.BujoLogic
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel

data class AddFastValueListState(
    override val ctx: ActorContext,
    override val trackerUser: TrackerUser,
    val templates: List<ValueTemplate>,
    val values: MutableList<Value> = mutableListOf()
) : ActorState(ctx, trackerUser)

@OptIn(ExperimentalCoroutinesApi::class)
object AddFastValueListActor : StateMachineActor<AddFastValueListState>(
    mandatoryStep({
        state.subActor = initChain(state, state.templates.firstOrNull(), state.templates.drop(1))
        true
    }, {
        logger.debug("Received a value: ${message.text}")
        if (state.subActor != DummyChannel) BujoLogic.handleSayActorMessage(message.text, state.subActor)
//         TODO: try to make it async and remove the sleep
        if (state.values.size >= state.templates.size - 1) Thread.sleep(2000)
//        var finished = false
//        state.subActor.invokeOnClose {
//            finished = state.subActor == DummyChannel
//        }
//        finished
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
            logger.debug("Received a value with type: ${state.name}(${state.type}) and value: ${state.value}")
            superState.values.add(
                Value(
                    state.type,
                    state.value
                )
            )
            superState.subActor = initChain(superState, templates.firstOrNull(), templates.drop(1))
        }
}
