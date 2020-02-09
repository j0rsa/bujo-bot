package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.api.RequestLens.habitLens
import com.j0rsa.bujo.telegram.api.RequestLens.multipleHabitsLens
import com.j0rsa.bujo.telegram.api.model.Habit
import com.j0rsa.bujo.telegram.api.model.HabitId
import com.j0rsa.bujo.telegram.api.model.HabitsInfo
import com.j0rsa.bujo.telegram.api.model.UserId
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

/**
 * @author red
 * @since 02.02.20
 */

object TrackerClient{
    private val client = OkHttp()
    fun ping(): Boolean =
        client(Request(Method.GET,"/health")).status == Status.OK

    fun getHabits(userId: UserId): List<HabitsInfo> {
        val request = Request(Method.GET, "/habits").with(userId)
        val response = client(request)
        return multipleHabitsLens(response)
    }

    fun getHabit(userId: UserId, habitId: HabitId): Habit =
        habitLens(client(Request(Method.GET, "/habits/$habitId").with(userId)))

    private fun Request.with(userId: UserId)=
        this.header("X-Auth-Id", userId.toString())
}