package com.j0rsa.bujo.telegram.actor.common

import com.j0rsa.bujo.telegram.BujoTalk
import com.j0rsa.bujo.telegram.Lines
import com.j0rsa.bujo.telegram.actor.common.StateMachineActor.Companion.sendLocalizedMessage
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

open class StateMachineActor<T : ActorState>(
    private val initStep: InitStep<T>,
    private vararg val steps: ActorStep<T>
) : Actor<T> {
    @OptIn(ObsoleteCoroutinesApi::class)
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
                                is OptionalStep<*> -> currentStep.skip(state).run { nextStep(message) }
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

        fun sendLocalizedMessage(
            state: ActorState,
            lines: List<KProperty1<Lines, String>>,
            replyMarkup: ReplyMarkup? = null
        ) =
            with(state) {
                ctx.bot.sendMessage(
                    chatId = ctx.chatId,
                    text = lines.joinToString(separator = "\n") { it.get(BujoTalk.withLanguage(user.language)) },
                    replyMarkup = replyMarkup
                ).let { true }
            }
    }
}

abstract class ActorState(
    open val ctx: ActorContext,
    val user: User = ctx.client.getUser(ctx.userId)
)

sealed class ActorStep<in T : ActorState>(
    private val action: StepDefinition<T>.() -> Boolean,
    private val caption: T.() -> Boolean = { true }
) {
    open operator fun invoke(state: T, message: ActorMessage.Say = ActorMessage.Say("")): Boolean =
        with(StepDefinition(state, message)) {
            action(this).also { caption(state) }
        }

    fun skip(state: T) = caption(state)
}

typealias InitStep<T> = MandatoryStep<T>

class OptionalStep<T : ActorState>(action: StepDefinition<T>.() -> Boolean, caption: T.() -> Boolean) :
    ActorStep<T>(action, caption)

class MandatoryStep<T : ActorState>(action: StepDefinition<T>.() -> Boolean) : ActorStep<T>(action)

object TerminateStep : ActorStep<ActorState>({
    sendLocalizedMessage(state, Lines::terminatorStepMessage)
    false
})


fun <T : ActorState> optionalStep(action: StepDefinition<T>.() -> Boolean, caption: T.() -> Boolean) =
    OptionalStep(action, caption)

fun <T : ActorState> initStep(action: StepDefinition<T>.() -> Boolean) =
    MandatoryStep(action)

fun <T : ActorState> mandatoryStep(action: StepDefinition<T>.() -> Boolean) =
    MandatoryStep(action)

data class StepDefinition<out T : ActorState>(val state: T, val message: ActorMessage.Say)