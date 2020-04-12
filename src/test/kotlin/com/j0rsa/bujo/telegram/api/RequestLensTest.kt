package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.Config
import com.j0rsa.bujo.telegram.api.model.HabitRequest
import com.j0rsa.bujo.telegram.api.model.Period
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldNot
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri

class RequestLensTest: ShouldSpec() {
    @Suppress("SameParameterValue")
    private fun contain(otherSting: String) = object : Matcher<String> {
        override fun test(value: String) = MatcherResult(value.contains(otherSting), "String $value should include $otherSting", "String $value should not include $otherSting")
    }

    init {

        should("Not contain nulls") {
            val habit = HabitRequest(
                "name",
                emptyList(),
                1,
                Period.Day,
                null,
                null,
                null,
                emptyList()
            )
            val request = Request(Method.GET, Uri.of(Config.app.tracker.url).path(""))
            val requestLens = RequestLens.habitRequestLens(habit, request)
            val body = (requestLens.body as MemoryBody).toString()

            body shouldNot contain("null")
        }

    }
}