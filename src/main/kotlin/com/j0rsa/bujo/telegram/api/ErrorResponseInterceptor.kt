package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.WithLogger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

object ErrorResponseInterceptor : Interceptor, WithLogger() {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()

        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            logger.error(
                """
                Received error response for ${response.request.url} with code ${response.code}
                Request body: ${(request.body)}
                Headers: ${response.headers}
                Body: $body
            """.trimIndent()
            )
            return response.newBuilder().body(body.toResponseBody(response.body?.contentType())).build();
        }
        return response
    }
}