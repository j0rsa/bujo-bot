package com.j0rsa.bujo.telegram.actor.common

import com.j0rsa.bujo.telegram.BujoTalk
import com.j0rsa.bujo.telegram.Lines
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
    private vararg val steps: ActorStep<T>
) : Actor<T>, Localized {
    private val logger = LoggerFactory.getLogger(this::class.java.name)

    @OptIn(ObsoleteCoroutinesApi::class)
    override fun yield(
        state: T,
        onCompletionHandler: (StateWithLocalization<T>.() -> Unit)?
    ): SendChannel<ActorMessage> =
        with(state.ctx.scope) {
            actor(onCompletion = { cause -> onCompletionHandler?.let { it(StateWithLocalization(state, cause)) } }) {
                val iterator = steps.iterator()
                fun nextStep(): ActorStep<T>? =
                    if (iterator.hasNext()) {
                        iterator.next().apply { init(state) }
                    } else {
                        null
                    }

                var currentStep = nextStep() ?: return@actor
                for (message in channel) {
                    currentStep = when (message) {
                        is ActorMessage.Say -> {
                            if (currentStep(state, message)) {
                                nextStep() ?: return@actor
                            } else currentStep
                        }
                        is ActorMessage.Skip -> {
                            when {
                                state.subActor != null -> {
                                    state.subActor?.send(ActorMessage.Skip)
                                    currentStep
                                }
                                currentStep is OptionalStep<*> -> nextStep() ?: return@actor
                                currentStep is MandatoryStep<*> -> {
                                    sendLocalizedMessage(state, Lines::stepCannotBeSkippedMessage)
                                    currentStep
                                }
                                else -> nextStep() ?: return@actor
                            }
                        }
                    }
                }
            }
        }
}

data class StateWithLocalization<T>(val state: T, val cause: Throwable?) : Localized

interface Localized {
    fun sendLocalizedMessage(
        state: ActorState,
        line: KProperty1<Lines, String>,
        replyMarkup: ReplyMarkup? = null
    ): Boolean =
        with(state) {
            ctx.bot.sendMessage(
                chatId = ctx.chatId,
                text = line.get(BujoTalk.withLanguage(trackerUser.language)),
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
                text = lines.joinToString(separator = "\n") { it.get(BujoTalk.withLanguage(trackerUser.language)) },
                replyMarkup = replyMarkup
            ).let { true }
        }
}

abstract class ActorState(
    open val ctx: ActorContext,
    val trackerUser: User = ctx.client.getUser(ctx.userId),
    var subActor: SendChannel<ActorMessage>? = null
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

data class StepSetupDefinition<out T : ActorState>(val state: T) : Localized
data class StepActionDefinition<out T : ActorState>(val state: T, val message: ActorMessage.Say) : Localized