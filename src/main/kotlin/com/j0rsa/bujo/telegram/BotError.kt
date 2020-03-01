package com.j0rsa.bujo.telegram

sealed class BotError {
	object NotFound : BotError()
	object NotCreated : BotError()
	data class SystemError(val message: String) : BotError()
}

typealias NotFound = BotError.NotFound
typealias NotCreated = BotError.NotCreated
typealias SystemError = BotError.SystemError