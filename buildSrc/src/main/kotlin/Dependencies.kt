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
    const val kapt = "1.3.61"
    const val telegramApi = "4.4.0"
    const val http4k = "3.231.0"
    const val config4k = "0.4.2"
    const val coroutines = "1.3.4"
    const val slf4j = "1.7.25"
    const val logback = "1.2.3"
    const val arrow = "0.10.4"

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
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val slf4jApi = "org.slf4j:slf4j-api:${Versions.slf4j}"
    const val logbackClassic = "ch.qos.logback:logback-classic:${Versions.logback}"
    const val logbackCore = "ch.qos.logback:logback-core:${Versions.logback}"
    const val arrowFx = "io.arrow-kt:arrow-fx:${Versions.arrow}"
    const val arrowSyntax = "io.arrow-kt:arrow-syntax:${Versions.arrow}"
    const val arrowMeta = "io.arrow-kt:arrow-meta:${Versions.arrow}"
}


object TestLibs {
    const val junit = "junit:junit:${Versions.junit}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    const val mockito = "org.mockito:mockito-core:3.2.4"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
    const val assertk = "com.willowtreeapps.assertk:assertk-jvm:0.21"
}