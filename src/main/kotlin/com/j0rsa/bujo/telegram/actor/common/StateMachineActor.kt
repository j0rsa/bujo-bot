package com.j0rsa.bujo.telegram.actor.common

import com.j0rsa.bujo.telegram.WithLogger
import com.j0rsa.bujo.telegram.api.model.TrackerUser
import com.j0rsa.bujo.telegram.bot.i18n.BujoTalk
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.monad.ActorContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.SelectClause2
import me.ivmg.telegram.entities.ReplyMarkup
import kotlin.reflect.KProperty1

/**
 * @author red
 * @since 01.03.20
 */

open class StateMachineActor<T : ActorState, OUT>(
    private val onComplete: T.() -> OUT,
    private vararg val steps: ActorStep<T>
) : Actor<T, OUT>, WithLogger() {
    @OptIn(ObsoleteCoroutinesApi::class)
    override fun yield(
        state: T,
        onCompletionHandler: (ContextualResult<OUT>.() -> Unit)?
    ): SendChannel<ActorMessage> =
        with(state.ctx.scope) {

            actor(
                onCompletion = { cause ->
                    onCompletionHandler?.let {
                        it(ContextualResult(state.ctx, state.trackerUser, onComplete(state), cause))
                    }
                }
            ) {
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
                            val success = currentStep(state, message)
                            message.result.complete(success)
                            if (success) {
                                nextStep() ?: return@actor
                            } else currentStep
                        }
                        is ActorMessage.Skip -> {
                            when {
                                state.subActor != DummyChannel -> {
                                    state.subActor.send(message)
                                    currentStep
                                }
                                currentStep is OptionalStep<*> -> {
                                    message.result.complete(true)
                                    nextStep() ?: return@actor
                                }
                                currentStep is MandatoryStep<*> -> {
                                    state.ctx.bot.sendMessage(
                                        state.ctx.chatId,
                                        BujoTalk.withLanguage(state.trackerUser.language)
                                            .stepCannotBeSkippedMessage
                                    )
                                    message.result.complete(false)
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

data class ContextualResult<OUT>(
    val ctx: ActorContext,
    private val trackerUser: TrackerUser,
    val result: OUT,
    val cause: Throwable?
) : Localized, WithLogger() {
    override fun context(): ActorContext = ctx
    override fun trackerUser(): TrackerUser = trackerUser
}

interface Localized {
    fun context(): ActorContext
    fun trackerUser(): TrackerUser
    fun sendLocalizedMessage(
        line: KProperty1<Lines, String>,
        replyMarkup: ReplyMarkup? = null,
        formatValues: List<String> = emptyList()
    ): Boolean =
        context().bot.sendMessage(
            chatId = context().chatId,
            text = line.get(BujoTalk.withLanguage(trackerUser().language)).format(*formatValues.toTypedArray()),
            replyMarkup = replyMarkup
        ).let { true }

    fun sendLocalizedMessage(
        lines: List<KProperty1<Lines, String>>,
        replyMarkup: ReplyMarkup? = null
    ) =
        context().bot.sendMessage(
            chatId = context().chatId,
            text = lines.joinToString(separator = "\n") { it.get(BujoTalk.withLanguage(trackerUser().language)) },
            replyMarkup = replyMarkup
        ).let { true }
}

abstract class ActorState(
    open val ctx: ActorContext,
    open val trackerUser: TrackerUser,
    var subActor: SendChannel<ActorMessage> = DummyChannel
)

@OptIn(ExperimentalCoroutinesApi::class)
object DummyChannel : SendChannel<ActorMessage> {
    override val isClosedForSend: Boolean = true
    override suspend fun send(element: ActorMessage) {}
    override val isFull = true
    override val onSend: SelectClause2<ActorMessage, SendChannel<ActorMessage>>
        get() = TODO("Not yet implemented")

    override fun close(cause: Throwable?): Boolean = true
    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
        handler(null)
    }

    override fun offer(element: ActorMessage): Boolean = true
}

sealed class ActorStep<in T : ActorState>(
    private val setup: StepSetupDefinition<T>.() -> Boolean,
    private val action: suspend StepActionDefinition<T>.() -> Boolean
): WithLogger() {
    fun init(state: T): Boolean = setup(StepSetupDefinition(state))
    suspend operator fun invoke(state: T, message: ActorMessage.Say = ActorMessage.Say("")): Boolean =
        action(StepActionDefinition(state, message))
}

class OptionalStep<T : ActorState>(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: suspend StepActionDefinition<T>.() -> Boolean
) :
    ActorStep<T>(setup, action)

class MandatoryStep<T : ActorState>(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: suspend StepActionDefinition<T>.() -> Boolean
) :
    ActorStep<T>(setup, action)

fun <T : ActorState> optionalStep(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: suspend StepActionDefinition<T>.() -> Boolean
) =
    OptionalStep(setup, action)

fun <T : ActorState> mandatoryStep(
    setup: StepSetupDefinition<T>.() -> Boolean,
    action: suspend StepActionDefinition<T>.() -> Boolean
) =
    MandatoryStep(setup, action)

abstract class Step<out T : ActorState>(private val state: T) : Localized, WithLogger() {
    override fun context(): ActorContext = state.ctx
    override fun trackerUser(): TrackerUser = state.trackerUser
}

data class StepSetupDefinition<out T : ActorState>(val state: T) : Step<T>(state)
data class StepActionDefinition<out T : ActorState>(val state: T, val message: ActorMessage.Say) : Step<T>(state)