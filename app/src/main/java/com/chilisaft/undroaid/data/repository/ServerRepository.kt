package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.app.ServerInformationQuery
import com.chilisaft.undroaid.data.models.Owner
import com.chilisaft.undroaid.data.models.Server
import javax.inject.Inject

class ServerRepository @Inject constructor(private val apolloClient: ApolloClient) {

    suspend fun getServerInformation(): Result<Server> {
        return try {
            val response = apolloClient.query(ServerInformationQuery()).execute()
            if (!response.hasErrors()) {
                response.data?.server?.let { serverData ->
                    Result.success(
                        Server(
                            owner = Owner(
                                userId = serverData.owner.userId,
                                username = serverData.owner.username,
                                url = serverData.owner.url,
                                avatar = serverData.owner.avatar
                            ),
                            guid = serverData.guid,
                            apiKey = serverData.apikey,
                            name = serverData.name,
                            wanIp = serverData.wanip,
                            lanIp = serverData.lanip,
                            localUrl = serverData.localurl,
                            remoteUrl = serverData.remoteurl
                        )
                    )
                } ?: Result.failure(Exception("Server data is null"))
            } else {
                Result.failure(Exception("GraphQL error: ${response.errors?.joinToString { it.message }}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}