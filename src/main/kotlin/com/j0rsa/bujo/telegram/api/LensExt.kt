package com.j0rsa.bujo.telegram.api

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure

/**
 * @author red
 * @since 21.03.20
 */

fun <IN, OUT> LensExtractor<IN, OUT>.toEither(): LensExtractor<IN, Either<Exception, OUT>> = object : LensExtractor<IN, Either<Exception, OUT>> {
    override fun invoke(target: IN): Either<Exception, OUT> = try {
        Right(this@toEither.invoke(target))
    } catch (e: LensFailure) {
        Left(e)
    }
}
