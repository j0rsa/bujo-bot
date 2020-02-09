package com.j0rsa.bujo.telegram.actor

import com.j0rsa.bujo.telegram.api.TrackerClient
import com.j0rsa.bujo.telegram.api.model.ActionRequest
import com.j0rsa.bujo.telegram.api.model.TagRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import me.ivmg.telegram.Bot
import org.http4k.core.Status

/**
 * @author red
 * @since 09.02.20
 */

object CreateActionActor : Actor {
    @UseExperimental(ObsoleteCoroutinesApi::class)
    override fun yield(scope: CoroutineScope, bot: Bot, chatId: Long, userId: Long) = with(scope) {
        actor<ActorMessage> {
            val user = TrackerClient.getUser(userId)
            var actionDescription = ""

            //INIT ACTOR
            bot.sendMessage(
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
                                bot.sendMessage(
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
                                when (TrackerClient.createAction(
                                    user.id, ActionRequest(
                                        actionDescription,
                                        actionTags
                                    )
                                ).status) {
                                    Status.OK, Status.CREATED ->
                                        bot.sendMessage(
                                            chatId,
                                            "Action was registered"
                                        )
                                    else ->
                                        bot.sendMessage(
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