package com.j0rsa.bujo.telegram.bot

import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.User
import java.util.*

/**
 * @author red
 * @since 12.03.20
 */

inline class BotUserId(val value: Long) {
    constructor(user: User) : this(user.id)
}

inline class ChatId(val value: Long) {
    constructor(message: Message) : this(message.chat.id)
}

inline class HabitId(val value: UUID) {
    companion object {
        fun randomValue() = HabitId(UUID.randomUUID())
    }
}

inline class ActionId(val value: UUID) {
    companion object {
        fun randomValue() =
            ActionId(UUID.randomUUID())

        fun fromString(s: String) =
            ActionId(UUID.fromString(s))
    }
}