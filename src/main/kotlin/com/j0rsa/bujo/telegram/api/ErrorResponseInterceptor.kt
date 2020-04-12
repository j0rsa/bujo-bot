package com.j0rsa.bujo.telegram.api

import com.j0rsa.bujo.telegram.WithLogger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

object ErrorResponseInterceptor: Interceptor, WithLogger() {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            logger.error("""
                Received error response for ${response.request.url} with code ${response.code}
                Headers: ${response.headers}
                Body: ${response.body?.byteString()}
            """.trimIndent())
        }
        return response
    }
}