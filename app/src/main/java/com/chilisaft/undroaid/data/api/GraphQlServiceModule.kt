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
    fun provideOkHttpClient(urlRewriteInterceptor: UrlRewriteInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(urlRewriteInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        authInterceptor: AuthInterceptor,
        okHttpClient: OkHttpClient
    ): ApolloClient {
        return ApolloClient.Builder()
            // This placeholder URL will be replaced by the UrlRewriteInterceptor
            .serverUrl("http://placeholder/graphql")
            .okHttpClient(okHttpClient)
            .addHttpInterceptor(authInterceptor)
            .build()
    }
}
