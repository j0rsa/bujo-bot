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
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty1

/**
 * @author red
 * @since 01.03.20
 */

open class StateMachineActor<T : ActorState>(
    private vararg val steps: ActorStep<T>,
    private val initStep: InitStep<T> = InitStep { true }
) : Actor<T> {
    private val logger = LoggerFactory.getLogger(this::class.java.name)

    @OptIn(ObsoleteCoroutinesApi::class)
    override fun yield(state: T): SendChannel<ActorMessage> =
        with(state.ctx.scope) {
            actor {
                val iterator = steps.iterator()
                fun nextStep(message: ActorMessage? = null): ActorStep<T> =
                    if (iterator.hasNext()) {
                        iterator.next()
                    } else {
                        TerminateStep()
                    }
                        .apply { if (this is TerminateStep) message?.complete() else message?.unComplete() }
                        .apply { init(state) }

                initStep.init(state)
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
        fun sendLocalizedMessage(
            state: ActorState,
            line: KProperty1<Lines, String>,
            replyMarkup: ReplyMarkup? = null
        ): Boolean =
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
    private val setup: StepSetupDefinition<T>.() -> Boolean,
    private val action: StepActionDefinition<T>.() -> Boolean
) {
    private val logger = LoggerFactory.getLogger(this::class.java.name)
    open fun init(state: T): Boolean = setup(StepSetupDefinition(state))
    open operator fun invoke(state: T, message: ActorMessage.Say = ActorMessage.Say("")): Boolean =
        action(StepActionDefinition(state, message))
}

class OptionalStep<T : ActorState>(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: StepActionDefinition<T>.() -> Boolean
) :
    ActorStep<T>(setup, action)

class MandatoryStep<T : ActorState>(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: StepActionDefinition<T>.() -> Boolean
) :
    ActorStep<T>(setup, action)

class TerminateStep<T : ActorState>(
    setup: T.() -> Boolean = { true }
) : ActorStep<T>({ setup(state) }, {
    sendLocalizedMessage(state, Lines::terminatorStepMessage)
    false
})

typealias InitStep<T> = TerminateStep<T>

fun <T : ActorState> optionalStep(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: StepActionDefinition<T>.() -> Boolean
) =
    OptionalStep(setup, action)

fun <T : ActorState> mandatoryStep(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: StepActionDefinition<T>.() -> Boolean
) =
    MandatoryStep(setup, action)

fun <T : ActorState> executionStep(exec: T.() -> Boolean) = TerminateStep(exec)

data class StepSetupDefinition<out T : ActorState>(val state: T)
data class StepActionDefinition<out T : ActorState>(val state: T, val message: ActorMessage.Say)