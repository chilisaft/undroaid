package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.chilisaft.undroaid.utils.Storage
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private var storage: Storage) : HttpInterceptor {

    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        return chain.proceed(request.newBuilder().addHeader("X-API-KEY", storage.apiToken ?: "").build())
    }
}