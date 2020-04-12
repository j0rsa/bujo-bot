package com.j0rsa.bujo.telegram.api

import com.google.gson.GsonBuilder
import org.http4k.format.ConfigurableGson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

object Gson : ConfigurableGson(
    GsonBuilder()
        .asConfigurable()
        .withStandardMappings()
        .done()
)