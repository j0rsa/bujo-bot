package com.j0rsa.bujo.telegram

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.io.File
import java.nio.charset.Charset

/**
 * @author red
 * @since 09.02.20
 */
  
object BujoTalk {
    private val translactions = Language.values().map {
        val lines = BujoTalk::class.java.classLoader
            .getResource("i18n/${it.name.toLowerCase()}_lines.conf")
            .file
            .let { fileName -> File(fileName) }
        it to ConfigFactory.parseFile(lines).extract<Lines>()
    }.toMap()

    fun getSupportedLanguageCodesWithFlags(): List<Pair<Language, String>> =
        listOf(
            Language.EN to "\uD83C\uDDEC\uD83C\uDDE7",
            Language.RU to "\uD83C\uDDF7\uD83C\uDDFA",
            Language.DE to "\uD83C\uDDE9\uD83C\uDDEA"
        )

    fun getWelcomeMessage(language: Language): String =
        translactions[language]!!.welcome

    fun getLanguage(languageCode: String?): Language =
        languageCode?.let {
            Language.valueOf(it.toUpperCase())
        } ?: Language.EN


}

data class Lines(
    val welcome: String
)

enum class Language{
    EN,
    RU,
    DE
}