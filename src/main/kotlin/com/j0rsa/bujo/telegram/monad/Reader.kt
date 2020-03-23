package com.j0rsa.bujo.telegram.monad

import arrow.core.Either
import arrow.fx.IO
import com.j0rsa.bujo.telegram.api.TrackerClient
import com.j0rsa.bujo.telegram.api.model.*
import com.j0rsa.bujo.telegram.bot.*
import kotlinx.coroutines.CoroutineScope
import org.http4k.core.Status

/**
 * @author red
 * @since 08.02.20
 */

class Reader<D, out A>(val run: (D) -> A) {

	inline fun <B> map(crossinline fa: (A) -> B): Reader<D, B> = Reader { d ->
		fa(run(d))
	}

	inline fun <B> flatMap(crossinline fa: (A) -> Reader<D, B>): Reader<D, B> = Reader { d ->
		fa(run(d)).run(d)
	}

	companion object {
		fun <D, A> just(a: A): Reader<D, A> = Reader { a }
		fun <D> ask(): Reader<D, D> = Reader { it }
	}
}

data class ActorContext(
	val chatId: ChatId,
	val userId: BotUserId,
	val bot: TelegramBot,
	val scope: CoroutineScope,
	val client: Client = TrackerClient
)

interface Client {
	fun health(): Boolean
	fun createUser(userRequest: CreateUserRequest): Pair<UserId?, Status>
	fun getHabits(userId: UserId): IO<List<HabitsInfo>>
	fun getUser(telegramUserId: BotUserId): IO<TrackerUser>
	fun getHabit(userId: UserId, habitId: HabitId): IO<HabitInfoView>
	fun createAction(userId: UserId, actionRequest: ActionRequest): Either<BotError, ActionId>
	fun addValue(userId: UserId, actionId: ActionId, value: Value): Either<BotError, ActionId>
	fun getAction(userId: UserId, actionId: ActionId): Either<BotError, Action>
	fun createHabit(userId: UserId, habit: HabitRequest): Either<BotError, HabitId>
}