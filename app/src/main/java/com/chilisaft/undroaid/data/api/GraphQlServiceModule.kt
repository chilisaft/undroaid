package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
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
            .build()
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        okHttpClient: OkHttpClient
    ): ApolloClient {
        // `ApolloClient.Builder().okHttpClient(...)` is a convenience that sets up its own
        // subscriptionNetworkTransport internally, which collides with configuring
        // webSocketServerUrl separately (crashes with "webSocketServerUrl has no effect if
        // subscriptionNetworkTransport is set"). Wiring the HTTP engine and the WebSocket
        // transport explicitly instead - both share the same OkHttpClient, so
        // UnraidInterceptor still rewrites the URL and attaches the API key for subscriptions
        // exactly as it does for queries/mutations.
        return ApolloClient.Builder()
            .serverUrl(PLACEHOLDER_URL)
            .httpEngine(DefaultHttpEngine { okHttpClient })
            .subscriptionNetworkTransport(
                WebSocketNetworkTransport.Builder()
                    .serverUrl(PLACEHOLDER_URL)
                    .webSocketEngine(WebSocketEngine { okHttpClient })
                    .build()
            )
            .build()
    }
}
