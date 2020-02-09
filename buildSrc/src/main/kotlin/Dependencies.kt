object App {
    const val compileVersion = "1.8"
}

object Plugins {
    const val gitProperties = "com.gorylenko.gradle-git-properties"
    const val dockerCompose = "com.avast.gradle.docker-compose"
    const val docker = "com.palantir.docker"
    const val shadow = "com.github.johnrengelman.shadow"
}

object Versions {
    const val kotlin = "1.3.61"
    const val telegramApi = "4.5.0"
    const val http4k = "3.231.0"
    const val config4k = "0.4.2"

    /* plugins */
    const val gitProperties = "1.4.17"
    const val dockerCompose = "0.9.4"
    const val docker = "0.24.0"
    const val shadow = "5.2.0"

    /* test */
    const val junit = "4.12"
}

object Libs {
    const val telegramApi = "io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:${Versions.telegramApi}"
    const val http4kClient = "org.http4k:http4k-client-okhttp:${Versions.http4k}"
    const val http4kFormat = "org.http4k:http4k-format-gson:${Versions.http4k}"
    const val config4k = "io.github.config4k:config4k:${Versions.config4k}"
}


object TestLibs {
    const val junit = "junit:junit:${Versions.junit}"
}