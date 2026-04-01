package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

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
        return ApolloClient.Builder()
            .serverUrl("https://placeholder.local/graphql")
            .okHttpClient(okHttpClient)
            .build()
    }
}
