package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.api.model.*
import org.http4k.core.Body
import org.http4k.format.Gson.auto

/**
 * @author red
 * @since 08.02.20
 */

object RequestLens {
    val habitInfoLens = Body.auto<HabitInfoView>().toLens()
    val habitLens = Body.auto<Habit>().toLens()
    val multipleHabitsLens = Body.auto<List<HabitsInfo>>().toLens()
    val tagLens = Body.auto<Tag>().toLens()
    val tagsLens = Body.auto<List<Tag>>().toLens()
    val actionLens = Body.auto<Action>().toLens()
    val actionIdLens = Body.auto<ActionId>().toLens()
    val actionRequestLens = Body.auto<ActionRequest>().toLens()
    val multipleActionLens = Body.auto<List<Action>>().toLens()
    val telegramUserIdLens = Body.auto<UserId>().toLens()
    val telegramUserCreateLens = Body.auto<CreateUserRequest>().toLens()
    val userLens = Body.auto<User>().toLens()
}