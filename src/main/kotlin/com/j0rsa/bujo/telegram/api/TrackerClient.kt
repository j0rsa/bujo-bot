package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.Config
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
import org.http4k.core.Uri

/**
 * @author red
 * @since 02.02.20
 */

object TrackerClient{
    private val client = OkHttp()
    fun health(): Boolean =
        client("/health".get()).status == Status.OK

    fun getHabits(userId: UserId): List<HabitsInfo> {
        val response = client("/habits".get().with(userId))
        return multipleHabitsLens(response)
    }

    fun getHabit(userId: UserId, habitId: HabitId): Habit =
        habitLens(client("/habits/$habitId".get().with(userId)))

    private fun Request.with(userId: UserId)=
        this.header(Config.app.tracker.authHeader, userId.toString())

    private fun String.get() =
        Request(Method.GET, Uri.of(Config.app.tracker.url).path(this))
    private fun String.post() =
        Request(Method.POST, Uri.of(Config.app.tracker.url).path(this))

}