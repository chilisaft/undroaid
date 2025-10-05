package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.chilisaft.undroaid.utils.Storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(private val storage: Storage) : HttpInterceptor {

    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        // This interceptor adds the API token to every request.
        // The URL is handled by the UrlRewriteInterceptor at the OkHttp level.
        val newRequest = request.newBuilder()
            .addHeader("X-API-KEY", storage.apiToken ?: "")
            .build()

        return chain.proceed(newRequest)
    }
}
