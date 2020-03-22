package com.j0rsa.bujo.telegram.api.model

import java.util.*

/**
 * @author red
 * @since 08.02.20
 */

data class CreateUserRequest(
    val telegramId: Long,
    val firstName: String = "",
    val lastName: String = "",
    val language: String = ""
)

data class TrackerUser(
    val id: UserId,
    val telegramId: Long,
    val firstName: String = "",
    val lastName: String = "",
    val language: String = ""
)

inline class UserId(val value: UUID) {
    companion object {
        fun randomValue() = UserId(UUID.randomUUID())
    }
}