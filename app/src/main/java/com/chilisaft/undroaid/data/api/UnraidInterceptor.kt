package com.chilisaft.undroaid.data.api

import com.chilisaft.undroaid.utils.Storage
import com.chilisaft.undroaid.utils.sanitizeUnraidUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
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

        if (!rawServerUrl.isNullOrBlank()) {
            val sanitizedUrl = rawServerUrl.sanitizeUnraidUrl()
            val newUrl = sanitizedUrl.toHttpUrlOrNull()
            
            if (newUrl != null) {
                builder.url(newUrl)
            } else if (overrideUrl != null) {
                throw IOException("Invalid Server URL: $sanitizedUrl")
            }
        }

        // 2. Handle API Key Override or from Storage
        val overrideToken = originalRequest.header("X-API-KEY-OVERRIDE")
        val finalToken = (overrideToken ?: storage.apiToken)?.trim()
        builder.removeHeader("X-API-KEY-OVERRIDE")

        if (!finalToken.isNullOrBlank()) {
            builder.header("X-API-KEY", finalToken)
        }

        return chain.proceed(builder.build())
    }
}
