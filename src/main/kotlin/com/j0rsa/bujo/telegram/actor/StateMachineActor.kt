package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.BujoTalk
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.StateMachineActor.Companion.sendLocalizedMessage
import com.j0rsa.bujo.telegram.api.model.User
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import me.ivmg.telegram.entities.ReplyMarkup
import kotlin.reflect.KProperty1

/**
 * @author red
 * @since 01.03.20
 */

@ObsoleteCoroutinesApi
open class StateMachineActor<T : ActorState>(
    private val initStep: InitStep<T>,
    private vararg val steps: ActorStep<T>
) : Actor<T> {
    override fun yield(state: T): SendChannel<ActorMessage> =
        with(state.ctx.scope) {
            actor {
                val iterator = steps.iterator()
                fun nextStep(message: ActorMessage? = null): ActorStep<T> =
                    if (iterator.hasNext()) {
                        message?.unComplete()
                        iterator.next()
                    } else {
                        message?.complete()
                        TerminateStep
                    }
                initStep(state)
                var currentStep = nextStep()
                for (message in channel) {
                    currentStep = when (message) {
                        is ActorMessage.Say -> {
                            if (currentStep(state, message)) {
                                nextStep(message)
                            } else currentStep
                        }
                        is ActorMessage.Skip -> {
                            when (currentStep) {
                                is OptionalStep<*> -> nextStep(message)
                                is MandatoryStep<*> -> {
                                    sendLocalizedMessage(state, Lines::stepCannotBeSkippedMessage)
                                    message.unComplete()
                                    currentStep
                                }
                                is TerminateStep -> {
                                    sendLocalizedMessage(state, Lines::terminatorStepMessage)
                                    message.completeExceptionally(
                                        IllegalArgumentException("Skip is not allowed for Terminate step")
                                    )
                                    currentStep
                                }
                            }
                        }
                    }
                }
            }
        }
    companion object {
        fun sendLocalizedMessage(state: ActorState, line: KProperty1<Lines, String>, replyMarkup: ReplyMarkup? = null) =
            with(state) {
                ctx.bot.sendMessage(
                    chatId = ctx.chatId,
                    text = line.get(BujoTalk.withLanguage(user.language)),
                    replyMarkup = replyMarkup
                    ).let { true }
            }
    }
}

abstract class ActorState(
    open val ctx: ActorContext,
    val user: User = ctx.client.getUser(ctx.userId)
)

sealed class ActorStep<in T : ActorState>(val body: StepDefinition<T>.() -> Boolean) {
    open operator fun invoke(state: T, message: ActorMessage.Say = ActorMessage.Say("")): Boolean =
        body(StepDefinition(state, message))
}

typealias InitStep<T> = MandatoryStep<T>
class OptionalStep<T : ActorState>(body: StepDefinition<T>.() -> Boolean) : ActorStep<T>(body)
class MandatoryStep<T : ActorState>(body: StepDefinition<T>.() -> Boolean) : ActorStep<T>(body)

@ObsoleteCoroutinesApi
object TerminateStep : ActorStep<ActorState>({
    sendLocalizedMessage(state, Lines::terminatorStepMessage)
    false
})


fun <T : ActorState> optionalStep(body: StepDefinition<T>.() -> Boolean) = OptionalStep(body)
fun <T : ActorState> initStep(body: StepDefinition<T>.() -> Boolean) = MandatoryStep(body)
fun <T : ActorState> mandatoryStep(body: StepDefinition<T>.() -> Boolean) = MandatoryStep(body)

data class StepDefinition<out T: ActorState>(val state: T, val message: ActorMessage.Say)