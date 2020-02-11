package com.j0rsa.bujo.telegram.actor

import arrow.core.Either.Right
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import com.j0rsa.bujo.telegram.monad.ActorContext
import com.j0rsa.bujo.telegram.monad.Reader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

/**
 * @author red
 * @since 09.02.20
 */

object CreateActionActor : Actor {
    @UseExperimental(ObsoleteCoroutinesApi::class)
    override fun yield(
        chatId: Long,
        userId: Long
    ): Reader<ActorContext, SendChannel<ActorMessage>> =
        Reader.ask<ActorContext>().map { ctx -> yield2(chatId, userId, ctx) }

    private fun yield2(chatId: Long, userId: Long, ctx: ActorContext) = with(ctx.scope) {
        actor<ActorMessage> {
            val user = ctx.client.getUser(userId)
            var actionDescription = ""

            //INIT ACTOR
            ctx.bot.sendMessage(
                chatId,
                "You are creating an action\n" +
                        "Enter action description"
            )
            //FINISH INIT
            var state: CreateActionState = CreateActionState.ActionDescription

            for (message in channel) {
                when (message) {
                    is ActorMessage.Say ->
                        when (state) {
                            CreateActionState.ActionDescription -> {
                                actionDescription = message.text
                                state = CreateActionState.ActionTags
                                //send message: Enter duration
                                ctx.bot.sendMessage(
                                    chatId,
                                    "Enter actions tags (comma separated)"
                                )
                                // flow is not finished
                                message.deferred.complete(false)
                            }
                            CreateActionState.ActionTags -> {
                                //received last item
                                val actionTags = message.text.split(",").map { TagRequest.fromString(it) }
                                //call API
                                when (ctx.client.createAction(
                                    user.id, ActionRequest(
                                        actionDescription,
                                        actionTags
                                    )
                                )) {
                                    is Right ->
                                        ctx.bot.sendMessage(
                                            chatId,
                                            "Action was registered"
                                        )
                                    else ->
                                        ctx.bot.sendMessage(
                                            chatId,
                                            "Action was not registered 😢"
                                        )
                                }
                                state = CreateActionState.Terminated
                                message.deferred.complete(true)
                            }

                            CreateActionState.Terminated -> {
                                message.deferred.completeExceptionally(IllegalStateException("We are done already!"))
                            }
                        }
                }
            }
        }
    }
}

sealed class CreateActionState {
    object ActionDescription : CreateActionState()
    object ActionTags : CreateActionState()
    object Terminated : CreateActionState()
}