package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.bot.Markup.valueMarkup
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext

data class AddFastValueState(
    override val ctx: ActorContext,
    override val trackerUser: TrackerUser,
    internal val type: ValueType,
    internal val name: String?,
    internal var value: String = ""
) : ActorState(ctx, trackerUser)

object AddFastValueActor : StateMachineActor<AddFastValueState, Value>(
    { Value(type, value, name) },
    mandatoryStep({
        sendLocalizedMessage(
            Lines::whatIsYourMessage,
            valueMarkup(state.type),
            listOf(state.name ?: state.type.caption.get(BujoTalk.withLanguage(state.trackerUser.language)))
        )
    }, {
        state.value = message.text
        true
    })
)
