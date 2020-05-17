package com.j0rsa.bujo.telegram.api

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.core.left
import arrow.core.right
import arrow.fx.IO
import arrow.fx.extensions.toIO
import com.j0rsa.bujo.telegram.Config
import com.j0rsa.bujo.telegram.WithLogger
import com.j0rsa.bujo.telegram.api.RequestLens.actionIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.actionLens
import com.j0rsa.bujo.telegram.api.RequestLens.actionRequestLens
import com.j0rsa.bujo.telegram.api.RequestLens.actionsLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitActionRequestLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitInfoLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitRequestLens
import com.j0rsa.bujo.telegram.api.RequestLens.multipleHabitsLens
import com.j0rsa.bujo.telegram.api.RequestLens.telegramUserCreateLens
import com.j0rsa.bujo.telegram.api.RequestLens.telegramUserIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.userLens
import com.j0rsa.bujo.telegram.api.RequestLens.valueRequestLens
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.bot.*
import com.j0rsa.bujo.telegram.monad.Client
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * @author red
 * @since 02.02.20
 */

object TrackerClient : Client, WithLogger() {
    private var httpLogging = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            logger.debug(message)
        }
    }).apply {
        level = HttpLoggingInterceptor.Level.valueOf(Config.app.httpLoggingLevel)
    }
    private val client = OkHttp(
        OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(httpLogging)
            .addInterceptor(ErrorResponseInterceptor)
            .build()
    )

    override fun health(): Boolean =
        client("/health".get()).status.code / 100 == 2

    override fun createUser(userRequest: CreateUserRequest): Pair<UserId?, Status> {
        val response = client(
            telegramUserCreateLens(userRequest, "/users".post())
        )
        return if (response.status.code > 299) null to response.status else
            telegramUserIdLens(response) to response.status
    }

    override fun getHabits(userId: UserId): IO<List<HabitsInfo>> {
        val response = client("/habits".get().with(userId))
        return multipleHabitsLens(response).toIO()
    }

    override fun getUser(telegramUserId: BotUserId): IO<TrackerUser> {
        val response = client("/users/${telegramUserId.value}".get())
        return userLens(response).toIO()
    }

    override fun getHabit(userId: UserId, habitId: HabitId): IO<HabitInfoView> =
        habitInfoLens(client("/habits/${habitId.value}".get().with(userId))).toIO()

    override fun deleteHabit(userId: UserId, habitId: HabitId): Either<BotError, Unit> =
        when(val status = client("/habits/${habitId.value}".delete().with(userId)).status) {
            Status.OK, Status.NO_CONTENT -> Either.right(Unit)
            else -> Either.left(BotError.SystemError(status.code.toString()))
        }

    override fun createHabit(userId: UserId, habit: HabitRequest): Either<BotError, HabitId> =
        with(client(habitRequestLens(habit, "/habits".post().with(userId)))) {
            when (status) {
                Status.OK, Status.CREATED -> habitIdLens(this).toBotEither().right()
                else -> NotCreated.left()
            }
        }.flatten()

    override fun createAction(userId: UserId, actionRequest: ActionRequest): Either<BotError, ActionId> =
        with(client(actionRequestLens(actionRequest, "/actions".post().with(userId)))) {
            return when (status) {
                Status.OK, Status.CREATED -> actionIdLens(this).toBotEither().right()
                else -> NotCreated.left()
            }.flatten()
        }

    override fun createHabitAction(userId: UserId, habitId: HabitId, actionRequest: HabitActionRequest): Either<BotError, ActionId> =
        with(client(habitActionRequestLens(actionRequest, "/actions/habit/${habitId.value}".post().with(userId)))) {
            return when (status) {
                Status.OK, Status.CREATED -> actionIdLens(this).toBotEither().right()
                else -> NotCreated.left()
            }.flatten()
        }

    override fun getAction(userId: UserId, actionId: ActionId): Either<BotError, Action> {
        val response = client("/actions/${actionId.value}".get().with(userId))
        return when (response.status) {
            Status.OK -> actionLens(response).toBotEither().right()
            Status.NOT_FOUND -> Either.Left(NotFound)
            else -> Either.Left(NotCreated)
        }.flatten()
    }

    private val actionDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun getHabitActions(userId: UserId, habitId: HabitId, date: LocalDate): Either<BotError, List<Action>> {
        val response = client("/actions/".get().with(userId)
            .query("habit", habitId.value.toString()).query("date", date.format(actionDateFormatter)))
        return when (response.status) {
            Status.OK -> actionsLens(response).toBotEither().right()
            Status.NOT_FOUND -> Either.Left(NotFound)
            else -> Either.Left(NotCreated)
        }.flatten()
    }

    override fun addValue(userId: UserId, actionId: ActionId, value: Value): Either<BotError, ActionId> {
        val response = client(valueRequestLens(value, "/actions/${actionId.value}/value".post().with(userId)))
        return when (response.status) {
            Status.OK, Status.CREATED -> actionIdLens(response).toBotEither().right()
            else -> Either.Left(NotCreated)
        }.flatten()
    }

    private fun Request.with(userId: UserId) =
        this.header(Config.app.tracker.authHeader, userId.value.toString())

    private fun String.get() =
        Request(Method.GET, Uri.of(Config.app.tracker.url).path(this))

    private fun String.post() =
        Request(Method.POST, Uri.of(Config.app.tracker.url).path(this))

    private fun String.delete() =
        Request(Method.DELETE, Uri.of(Config.app.tracker.url).path(this))

    private fun <T> Either<Exception, T>.toBotEither(): Either<BotError, T> =
        this.mapLeft { BotError.SystemError(it.message ?: "Unknown error") }
}