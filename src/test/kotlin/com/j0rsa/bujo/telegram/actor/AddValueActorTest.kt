package com.j0rsa.bujo.telegram.actor

import arrow.core.right
import arrow.fx.extensions.toIO
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.j0rsa.bujo.telegram.bot.Markup.valueMarkup
import com.j0rsa.bujo.telegram.bot.Markup.valueTypeMarkup
import com.j0rsa.bujo.telegram.bot.i18n.Lines
import com.j0rsa.bujo.telegram.actor.common.ActorMessage
import com.j0rsa.bujo.telegram.api.model.ValueType
import com.j0rsa.bujo.telegram.monad.Client
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddValueActorTest : ActorBotTest() {
	private val defaultType = ValueType.Mood

	@Test
	fun testSuccessAddValue() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user.right().toIO()
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), trackerUser))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(sayValue())

		verify(client).getUser(userId)
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueInitMessage),
			replyMarkup = valueTypeMarkup("en")
		)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueNameMessage, Lines::orTapSkipMessage, format = "%s\n%s"))
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueValueMessage),
			replyMarkup = defaultValueMarkup()
		)
		assertThat(actorChannel.isClosedForSend).isTrue()
	}

	@Test
	fun typeCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user.right().toIO()
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), trackerUser))
		actorChannel.send(skip())

		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueInitMessage),
			replyMarkup = valueTypeMarkup("en")
		)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
		assertThat(actorChannel.isClosedForSend).isFalse()
		actorChannel.close()
	}

	@Test
	fun valueCanNotBeSkipped() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user.right().toIO()
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), trackerUser))
		actorChannel.send(sayType())
		actorChannel.send(sayName())
		actorChannel.send(skip())
		actorChannel.send(sayValue())

		verify(client).getUser(userId)
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueInitMessage),
			replyMarkup = valueTypeMarkup("en")
		)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueNameMessage, Lines::orTapSkipMessage))
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueValueMessage),
			replyMarkup = defaultValueMarkup()
		)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::stepCannotBeSkippedMessage))
		assertThat(actorChannel.isClosedForSend).isTrue()
	}

	@Test
	fun whenSkipNameThenAddWithDefaultName() = runBlockingTest {
		val client = mock<Client> {
			on { getUser(userId) } doReturn user.right().toIO()
		}

		val trackerUser = client.getUser(userId).unsafeRunSync()
		val actorChannel = AddValueActor.yield(AddValueState(actorContext(client), trackerUser))
		actorChannel.send(sayType())
		actorChannel.send(skip())
		actorChannel.send(sayValue())

		verify(client).getUser(userId)
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueInitMessage),
			replyMarkup = valueTypeMarkup("en")
		)
		verify(bot).sendMessage(chatId, getLocalizedMessage(Lines::addActionValueNameMessage, Lines::orTapSkipMessage))
		verify(bot).sendMessage(
			chatId,
			getLocalizedMessage(Lines::addActionValueValueMessage),
			replyMarkup = defaultValueMarkup()
		)
		assertThat(actorChannel.isClosedForSend).isTrue()
	}

	private fun sayType() = ActorMessage.Say(defaultType.name)
	private fun sayName() = ActorMessage.Say("Mood")
	private fun sayValue() = ActorMessage.Say("5")

	private fun defaultValueMarkup() = valueMarkup(defaultType)
}