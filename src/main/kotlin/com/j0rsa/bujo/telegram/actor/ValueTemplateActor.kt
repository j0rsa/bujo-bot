package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorState
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor
import com.j0rsa.bujo.telegram.actor.common.mandatoryStep
import com.j0rsa.bujo.telegram.actor.common.optionalStep
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.monad.ActorContext

/**
 * @author red
 * @since 15.03.20
 */

data class ValueTemplateState(
    override val ctx: ActorContext,
    var type: ValueType,
    var name: String? = null
) : ActorState(ctx)

class ValueTemplateActor : StateMachineActor<ValueTemplateState>(
    mandatoryStep({
        sendLocalizedMessage(state, Lines::chooseTemplateValueTypeMessage)
    }, {
        state.type = ValueType.valueOf(message.text.trim())
        true
    }),
    optionalStep({
        sendLocalizedMessage(
            state,
            listOf(
                Lines::inputTemplateValueNameWithDefaultMessage,
                Lines::orTapSkipMessage
            )
        )
    }, {
        state.name = message.text
        true
    })
)