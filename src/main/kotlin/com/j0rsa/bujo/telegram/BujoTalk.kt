package com.j0rsa.bujo.telegram

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

/**
 * @author red
 * @since 09.02.20
 */
  
object BujoTalk {
    private val translations = Language.values().map {
        val classLoader = BujoTalk::class.java.classLoader
        val resourceName = "i18n/${it.name.toLowerCase()}_lines.conf"
        it to ConfigFactory.parseResources(classLoader, resourceName).extract<Lines>()
    }.toMap()

    fun getSupportedLanguageCodesWithFlags(): List<Pair<Language, String>> =
        listOf(
            Language.EN to "\uD83C\uDDEC\uD83C\uDDE7",
            Language.RU to "\uD83C\uDDF7\uD83C\uDDFA",
            Language.DE to "\uD83C\uDDE9\uD83C\uDDEA"
        )

    private fun getLanguage(languageCode: String?): Language =
        languageCode?.let {
            Language.valueOf(it.toUpperCase())
        } ?: Language.EN

    fun withLanguage(languageCode: String?): Lines =
        translations[getLanguage(languageCode)]!!
}

data class Lines(
    val welcome: String,
    val showHabitsMessage: String,
    val showHabitsButton: String,
    val createActionButton: String,
    val welcomeBack: String,
    val genericError: String
)

enum class Language{
    EN,
    RU,
    DE
}