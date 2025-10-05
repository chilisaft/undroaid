package com.chilisaft.undroaid.data.api

import com.chilisaft.undroaid.utils.Storage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlRewriteInterceptor @Inject constructor(private val storage: Storage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val serverUrl = storage.serverUrl

        // We need to allow the first request to go through, which is the login request.
        // For that one, we don't have a serverUrl yet.
        if (serverUrl.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val newUrl = (serverUrl + originalRequest.url.encodedPath).toHttpUrlOrNull()
            ?: throw IOException("Invalid Server URL: $serverUrl")

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
