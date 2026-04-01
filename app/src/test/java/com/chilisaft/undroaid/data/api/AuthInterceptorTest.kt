package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.chilisaft.undroaid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AuthInterceptorTest {

    @Test
    fun `interceptor adds api key to header`() = runBlocking {
        val storage = mockk<Storage>()
        val apiToken = "test-api-token"
        every { storage.apiToken } returns apiToken

        val interceptor = AuthInterceptor(storage)

        val request = HttpRequest.Builder(
            method = HttpMethod.Post,
            url = "http://localhost/graphql"
        ).build()

        val chain = mockk<HttpInterceptorChain>(relaxed = true)

        interceptor.intercept(request, chain)

        coVerify {
            chain.proceed(withArg { interceptedRequest ->
                assertThat(interceptedRequest.headers.first { it.name == "X-API-KEY" }.value).isEqualTo(apiToken)
            })
        }
    }

    @Test
    fun `interceptor does not add header for null api key`() = runBlocking {
        val storage = mockk<Storage>()
        every { storage.apiToken } returns null

        val interceptor = AuthInterceptor(storage)

        val request = HttpRequest.Builder(
            method = HttpMethod.Post,
            url = "http://localhost/graphql"
        ).build()

        val chain = mockk<HttpInterceptorChain>(relaxed = true)

        interceptor.intercept(request, chain)

        coVerify {
            chain.proceed(withArg { interceptedRequest ->
                assertThat(interceptedRequest.headers.any { it.name == "X-API-KEY" }).isFalse()
            })
        }
    }
}