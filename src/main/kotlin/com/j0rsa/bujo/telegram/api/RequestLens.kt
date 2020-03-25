package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.bot.ActionId
import com.j0rsa.bujo.telegram.bot.HabitId
import com.j0rsa.bujo.telegram.api.model.*
import org.http4k.core.Body
import org.http4k.format.Gson.auto

/**
 * @author red
 * @since 08.02.20
 */

object RequestLens {
    val habitInfoLens = Body.auto<HabitInfoView>().toLens().toEither()
    val habitLens = Body.auto<Habit>().toLens().toEither()
    val habitIdLens = Body.auto<HabitId>().toLens().toEither()
    val habitRequestLens = Body.auto<HabitRequest>().toLens()
    val multipleHabitsLens = Body.auto<List<HabitsInfo>>().toLens().toEither()
    val tagLens = Body.auto<Tag>().toLens()
    val tagsLens = Body.auto<List<Tag>>().toLens()
    val actionLens = Body.auto<Action>().toLens().toEither()
    val actionIdLens = Body.auto<ActionId>().toLens().toEither()
    val actionRequestLens = Body.auto<ActionRequest>().toLens()
    val habitActionRequestLens = Body.auto<HabitActionRequest>().toLens()
    val valueRequestLens = Body.auto<Value>().toLens()
    val multipleActionLens = Body.auto<List<Action>>().toLens()
    val telegramUserIdLens = Body.auto<UserId>().toLens()
    val telegramUserCreateLens = Body.auto<CreateUserRequest>().toLens()
    val userLens = Body.auto<TrackerUser>().toLens().toEither()
}