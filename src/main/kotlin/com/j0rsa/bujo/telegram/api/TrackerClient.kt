package com.j0rsa.bujo.telegram.api

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.j0rsa.bujo.telegram.*
import com.j0rsa.bujo.telegram.api.RequestLens.actionIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.actionLens
import com.j0rsa.bujo.telegram.api.RequestLens.actionRequestLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitLens
import com.j0rsa.bujo.telegram.api.RequestLens.habitRequestLens
import com.j0rsa.bujo.telegram.api.RequestLens.multipleHabitsLens
import com.j0rsa.bujo.telegram.api.RequestLens.telegramUserCreateLens
import com.j0rsa.bujo.telegram.api.RequestLens.telegramUserIdLens
import com.j0rsa.bujo.telegram.api.RequestLens.userLens
import com.j0rsa.bujo.telegram.api.RequestLens.valueRequestLens
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.monad.Client
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

object TrackerClient : Client {
	private val logger = getLogger(this::class.java.name)
	var httpLogging = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
		override fun log(message: String) {
			logger.debug(message)
		}
	}).apply { setLevel(HttpLoggingInterceptor.Level.valueOf(Config.app.httpLoggingLevel)) }
	private val client = OkHttp(
		OkHttpClient.Builder()
			.followRedirects(false)
			.addInterceptor(httpLogging)
			.build()
	)

	override fun health(): Boolean =
		client("/health".get()).status == Status.OK

	override fun createUser(userRequest: CreateUserRequest): Pair<UserId?, Status> {
		val response = client(
			telegramUserCreateLens(userRequest, "/users".post())
		)
		return if (response.status.code > 299) null to response.status else
			telegramUserIdLens(response) to response.status
	}

	override fun getHabits(userId: UserId): List<HabitsInfo> {
		val response = client("/habits".get().with(userId))
		return multipleHabitsLens(response)
	}

	override fun getUser(telegramUserId: BotUserId): User {
		val response = client("/users/${telegramUserId.value}".get())
		return userLens(response)
	}

	override fun getHabit(userId: UserId, habitId: HabitId): Habit =
		habitLens(client("/habits/$habitId".get().with(userId)))

	override fun createHabit(userId: UserId, habit: HabitRequest): Either<BotError, HabitId> =
		with(client(habitRequestLens(habit, "/habits".post().with(userId)))) {
			when (status) {
				Status.OK, Status.CREATED -> habitIdLens(this).right()
				else -> NotCreated.left()
			}
		}

	override fun createAction(userId: UserId, actionRequest: ActionRequest): Either<BotError, ActionId> =
		with(client(actionRequestLens(actionRequest, "/actions".post().with(userId)))) {
			return when (status) {
				Status.OK, Status.CREATED -> Either.Right(actionIdLens(this))
				else -> Either.Left(NotCreated)
			}
		}

	override fun getAction(userId: UserId, actionId: ActionId): Either<BotError, Action> {
		val response = client("/actions/${actionId.value}".get().with(userId))
		return when (response.status) {
			Status.OK -> Either.Right(actionLens(response))
			Status.NOT_FOUND -> Either.Left(NotFound)
			else -> Either.Left(NotCreated)
		}
	}

	override fun addValue(userId: UserId, actionId: ActionId, value: Value): Either<BotError, ActionId> {
		val response = client(valueRequestLens(value, "/actions/${actionId.value}/value".post().with(userId)))
		return when (response.status) {
			Status.OK, Status.CREATED -> Either.Right(actionIdLens(response))
			else -> Either.Left(NotCreated)
		}
	}

	private fun Request.with(userId: UserId) =
		this.header(Config.app.tracker.authHeader, userId.value.toString())

	private fun String.get() =
		Request(Method.GET, Uri.of(Config.app.tracker.url).path(this))

	private fun String.post() =
		Request(Method.POST, Uri.of(Config.app.tracker.url).path(this))
}