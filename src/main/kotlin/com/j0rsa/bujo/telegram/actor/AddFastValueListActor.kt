package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.*
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueTemplate
import com.j0rsa.bujo.telegram.bot.BujoLogic
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.channels.SendChannel

data class AddFastValueListState(
    override val ctx: ActorContext,
    override val trackerUser: TrackerUser,
    val templates: List<ValueTemplate>,
    val values: MutableList<Value> = mutableListOf()
) : ActorState(ctx, trackerUser)

object AddFastValueListActor : StateMachineActor<AddFastValueListState>(
    mandatoryStep({
        state.subActor = initChain(state, state.templates.firstOrNull(), state.templates.drop(1))
        true
    }, {
        if (state.subActor != DummyChannel) BujoLogic.handleSayActorMessage(message.text, state.subActor)
        Thread.sleep(1000)
        state.subActor == DummyChannel
    })
)

fun initChain(superState: AddFastValueListState, currentTemplate: ValueTemplate?, templates: List<ValueTemplate>): SendChannel<ActorMessage> {
    return if (currentTemplate == null) DummyChannel else
    AddFastValueActor
        .yield(
            AddFastValueState(
                superState.ctx,
                superState.trackerUser,
                currentTemplate.type,
                currentTemplate.name
            )
        ) {
            logger.info("Received a value with type: ${state.name}(${state.type}) and value: ${state.value}")
            superState.values.add(
                Value(
                    state.type,
                    state.value
                )
            )
            superState.subActor = initChain(superState, templates.firstOrNull(), templates.drop(1))
        }
}
