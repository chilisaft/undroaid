package com.chilisaft.undroid.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.DefaultHttpEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

private const val PLACEHOLDER_URL = "https://placeholder.local/graphql"

@Module
@InstallIn(SingletonComponent::class)
object GraphQlServiceModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        unraidInterceptor: UnraidInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(unraidInterceptor)
            .addNetworkInterceptor(CleartextGuardInterceptor())
            // The API key is a custom header, which OkHttp keeps on redirects - even cross-host or
            // https -> http ones (it only strips `Authorization`). The GraphQL endpoint never needs
            // to redirect, so don't follow any: a redirect surfaces as an error naming its target.
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        okHttpClient: OkHttpClient,
        apiKeyStatus: ApiKeyStatus
    ): ApolloClient {
        // The real server URL and API key are applied per request by UnraidInterceptor, so the
        // builder only needs a placeholder. No subscription (WebSocket) transport: the app polls
        // instead of subscribing (see ServerRepository.observeSystemMetricsPoll) - if that ever
        // changes, wire one with `subscriptionNetworkTransport(...)` sharing this OkHttpClient,
        // not `okHttpClient(...)`/`webSocketServerUrl(...)`, which conflict at build time.
        return ApolloClient.Builder()
            .serverUrl(PLACEHOLDER_URL)
            .httpEngine(DefaultHttpEngine { okHttpClient })
            .addInterceptor(KeyRejectedInterceptor(apiKeyStatus))
            .build()
    }
}
