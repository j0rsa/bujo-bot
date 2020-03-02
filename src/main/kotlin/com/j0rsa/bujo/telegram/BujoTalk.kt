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
            if (it.isNotBlank()) Language.valueOf(it.toUpperCase()) else Language.EN
        } ?: Language.EN

    fun withLanguage(languageCode: String?): Lines =
        translations[getLanguage(languageCode)] ?: error("With some magic language was not found")
}

data class Lines(
    val welcome: String,
    val showHabitsMessage: String,
    val showHabitsButton: String,
    val createActionButton: String,
    val welcomeBack: String,
    val genericError: String,
    val actionNotRegisteredMessage: String,
    val actionRegisteredMessage: String,
    val stepCannotBeSkippedMessage: String,
    val actionCreationDescriptionInput: String,
    val actionCreationTagsInput: String,
    val actionCreationInitMessage: String,
    val terminatorStepMessage: String,
    val addActionValueInitMessage: String,
    val addActionValueNameMessage: String,
    val addActionValueValueMessage: String,
    val addActionValueRegistered: String,
    val addActionValueNotRegistered: String,
    val createHabitInitMessage: String,
    val createHabitDurationMessage: String
)

enum class Language{
    EN,
    RU,
    DE
}