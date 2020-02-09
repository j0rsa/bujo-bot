package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.Config
import com.j0rsa.bujo.telegram.api.RequestLens.actionRequestLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitLens
import com.j0rsa.bujo.telegram.api.RequestLens.multipleHabitsLens
import com.j0rsa.bujo.telegram.api.RequestLens.telegramUserCreateLens
import com.j0rsa.bujo.telegram.api.RequestLens.telegramUserIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.userLens
import com.j0rsa.bujo.telegram.api.model.*
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import okio.GzipSource
import okio.buffer
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.slf4j.LoggerFactory.getLogger

/**
 * @author red
 * @since 02.02.20
 */

object TrackerClient {
    private val logger = getLogger(this::javaClass.name)
    private val logging = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BASIC);
    }
    private val client = OkHttp(OkHttpClient.Builder()
        .followRedirects(false)
        .addInterceptor(logging)
        .build())
    fun health(): Boolean =
        client("/health".get()).status == Status.OK

    fun createUser(userRequest: CreateUserRequest): Pair<UserId?, Status> {
        val response = client(
            telegramUserCreateLens(userRequest, "/users".post())
        )
        return if (response.status.code > 299) null to response.status else
            telegramUserIdLens(response) to response.status
    }

    fun getHabits(userId: UserId): List<HabitsInfo> {
        val response = client("/habits".get().with(userId))
        return multipleHabitsLens(response)
    }

    fun getUser(telegramUserId: Long): User {
        val response = client("/users/$telegramUserId".get())
        return userLens(response)
    }

    fun getHabit(userId: UserId, habitId: HabitId): Habit =
        habitLens(client("/habits/$habitId".get().with(userId)))

    fun createAction(userId: UserId, actionRequest: ActionRequest) =
        client(actionRequestLens(actionRequest, "/actions".post().with(userId)))

    private fun Request.with(userId: UserId) =
        this.header(Config.app.tracker.authHeader, userId.value.toString())

    private fun String.get() =
        Request(Method.GET, Uri.of(Config.app.tracker.url).path(this))

    private fun String.post() =
        Request(Method.POST, Uri.of(Config.app.tracker.url).path(this))

    private fun RequestBody?.utf8String(): String =
        this?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            buffer.readUtf8()
        } ?: ""

    private fun ResponseBody?.utf8String(): String =
        this?.let {
            GzipSource(it.source()).buffer().readUtf8()
        } ?: ""
}