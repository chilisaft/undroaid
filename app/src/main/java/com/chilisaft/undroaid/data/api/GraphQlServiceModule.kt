package com.chilisaft.undroaid.data.api

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.utils.Storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import javax.inject.Inject

@Module
@InstallIn(ActivityRetainedComponent::class)
class GraphQlServiceModule @Inject constructor() {

    @Provides
    fun provideGraphQlServiceModule (authInterceptor: AuthInterceptor, storage: Storage): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(storage.serverUrl + "/graphql")
            .addHttpInterceptor(authInterceptor)
            .build()
    }
}