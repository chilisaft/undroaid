package com.chilisaft.undroid.data.api

import com.chilisaft.undroid.utils.Storage
import com.chilisaft.undroid.utils.sanitizeUnraidUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnraidInterceptor @Inject constructor(private val storage: Storage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // 1. Handle Server URL Override or from Storage
        val overrideUrl = originalRequest.header("X-Server-Url")
        val rawServerUrl = (overrideUrl ?: storage.serverUrl)
        builder.removeHeader("X-Server-Url")

        // No usable server URL means the request must not go out at all - previously it silently
        // fell through to ApolloClient's placeholder URL, still carrying the API key.
        if (rawServerUrl.isNullOrBlank()) throw InvalidServerUrlException("(none)")
        val sanitizedUrl = rawServerUrl.sanitizeUnraidUrl()
        builder.url(sanitizedUrl.toHttpUrlOrNull() ?: throw InvalidServerUrlException(sanitizedUrl))

        // 2. Handle API Key Override or from Storage
        val overrideToken = originalRequest.header("X-API-KEY-OVERRIDE")
        val finalToken = (overrideToken ?: storage.apiToken)?.trim()
        builder.removeHeader("X-API-KEY-OVERRIDE")

        if (!finalToken.isNullOrBlank()) {
            builder.header("X-API-KEY", finalToken)
        }

        // 3. A longer read timeout for operations the server takes minutes to answer (pulling
        // container images) - everything else keeps OkHttp's 10 s default.
        val readTimeoutSeconds = originalRequest.header(READ_TIMEOUT_HEADER)?.toIntOrNull()
        builder.removeHeader(READ_TIMEOUT_HEADER)
        val timedChain = if (readTimeoutSeconds != null) chain.withReadTimeout(readTimeoutSeconds, TimeUnit.SECONDS) else chain

        return timedChain.proceed(builder.build())
    }

    companion object {
        /** Set on a request (in seconds) to raise its read timeout; stripped before it's sent. */
        const val READ_TIMEOUT_HEADER = "X-Read-Timeout-Seconds"
    }
}
