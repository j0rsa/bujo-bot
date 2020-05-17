package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.bot.ActionId
import com.j0rsa.bujo.telegram.bot.HabitId
import com.j0rsa.bujo.telegram.api.model.*

/**
 * @author red
 * @since 08.02.20
 */

object RequestLens {
    val habitInfoLens = Gson.autoBody<HabitInfoView>().toLens().toEither()
    val habitLens = Gson.autoBody<Habit>().toLens().toEither()
    val habitIdLens = Gson.autoBody<HabitId>().toLens().toEither()
    val habitRequestLens = Gson.autoBody<HabitRequest>().toLens()
    val multipleHabitsLens = Gson.autoBody<List<HabitsInfo>>().toLens().toEither()
    val tagLens = Gson.autoBody<Tag>().toLens()
    val tagsLens = Gson.autoBody<List<Tag>>().toLens()
    val actionLens = Gson.autoBody<Action>().toLens().toEither()
    val actionsLens = Gson.autoBody<List<Action>>().toLens().toEither()
    val actionIdLens = Gson.autoBody<ActionId>().toLens().toEither()
    val actionRequestLens = Gson.autoBody<ActionRequest>().toLens()
    val habitActionRequestLens = Gson.autoBody<HabitActionRequest>().toLens()
    val valueRequestLens = Gson.autoBody<Value>().toLens()
    val multipleActionLens = Gson.autoBody<List<Action>>().toLens()
    val telegramUserIdLens = Gson.autoBody<UserId>().toLens()
    val telegramUserCreateLens = Gson.autoBody<CreateUserRequest>().toLens()
    val userLens = Gson.autoBody<TrackerUser>().toLens().toEither()
}