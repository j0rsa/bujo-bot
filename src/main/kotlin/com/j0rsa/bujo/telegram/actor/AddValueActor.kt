package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.actor.common.optionalStep
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.api.model.Value
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.bot.Markup.valueMarkup
import com.j0rsa.bujo.telegram.bot.Markup.valueTypeMarkup
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext

data class AddValueState(
    override val ctx: ActorContext,
    override val trackerUser: TrackerUser,
    var type: ValueType = ValueType.values().random(),
    var name: String = "",
    var value: String = ""
) : ActorState(ctx, trackerUser)

object AddValueActor : StateMachineActor<AddValueState, Value>(
    { Value(type, value, name) },
    mandatoryStep({
        sendLocalizedMessage(Lines::addActionValueInitMessage, valueTypeMarkup(state.trackerUser.language))
    }, {
        try {
            state.type = ValueType.valueOf(message.text)
        } catch (e: Exception) {
            return@mandatoryStep false
        }
        true
    }),
    optionalStep({
        state.name = state.type.name
        sendLocalizedMessage(listOf(Lines::addActionValueNameMessage, Lines::orTapSkipMessage))
    }, {
        state.name = message.text
        true
    }),
    mandatoryStep({
        sendLocalizedMessage(Lines::addActionValueValueMessage, valueMarkup(state.type))
    }, {
        state.value = message.text
        true
    })
)
