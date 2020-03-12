package com.j0rsa.bujo.telegram

import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.User

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
