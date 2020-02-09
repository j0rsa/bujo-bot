package com.j0rsa.bujo.telegram.api.model

import java.util.*

/**
 * @author red
 * @since 08.02.20
 */

data class TagRequest(
    val name: String
) {
    companion object {
        fun fromString(string: String) = TagRequest(string.trim())
    }
}

data class Tag(
    val name: String,
    val id: TagId? = null
)

inline class TagId(val value: UUID) {
    companion object {
        fun randomValue() = TagId(UUID.randomUUID())
    }
}