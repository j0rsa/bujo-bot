package com.j0rsa.bujo.telegram

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

/**
 * @author red
 * @since 08.02.20
 */

object Config {
    val app: AppConfig = ConfigFactory.load().extract("app")
}

data class AppConfig(
    val token: String,
    val tracker: Tracker
)

data class Tracker(
    val url: String,
    val authHeader: String
)
