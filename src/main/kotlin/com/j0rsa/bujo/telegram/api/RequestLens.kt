package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.api.model.*
import org.http4k.core.Body
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.uuid
import org.http4k.format.Gson.auto

/**
 * @author red
 * @since 08.02.20
 */


object RequestLens {
    val habitInfoLens = Body.auto<HabitInfoView>().toLens()
    val habitLens = Body.auto<Habit>().toLens()
    val multipleHabitsLens = Body.auto<List<HabitsInfo>>().toLens()
    val habitIdLens = Path.uuid().map(::HabitId).of("id")
    val userLens = Header.uuid().map(::UserId).required("X-Auth-Id")
    val tagLens = Body.auto<Tag>().toLens()
    val tagsLens = Body.auto<List<Tag>>().toLens()
    val actionLens = Body.auto<Action>().toLens()
    val multipleActionLens = Body.auto<List<Action>>().toLens()
    val actionIdLens = Path.uuid().map(::ActionId).of("id")
}