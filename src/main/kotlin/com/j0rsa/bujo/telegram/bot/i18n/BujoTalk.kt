package com.j0rsa.bujo.telegram.bot.i18n

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

/**
 * @author red
 * @since 09.02.20
 */
  
object BujoTalk {
    private val translations = Language.values().map(
        BujoTalk::loadLinesForLanguage
    ).toMap()

    private fun loadLinesForLanguage(language: Language): Pair<Language, Lines> {
        val classLoader = BujoTalk::class.java.classLoader
        val resourceName = "i18n/${language.name.toLowerCase()}_lines.conf"
        return language to ConfigFactory.parseResources(classLoader, resourceName).extract()
    }

    fun getSupportedLanguageCodesWithFlags(): List<Pair<Language, String>> =
        listOf(
            Language.EN to "\uD83C\uDDEC\uD83C\uDDE7",
            Language.RU to "\uD83C\uDDF7\uD83C\uDDFA",
            Language.DE to "\uD83C\uDDE9\uD83C\uDDEA"
        )

    private fun getLanguage(languageCode: String?): Language =
        languageCode?.let {
            if (it.isNotBlank()) Language.valueOf(it.toUpperCase()) else Language.EN
        } ?: Language.EN

    fun withLanguage(languageCode: String?): Lines =
        translations[getLanguage(
            languageCode
        )] ?: error("With some magic language was not found")
}

enum class Language{
    EN,
    RU,
    DE
}