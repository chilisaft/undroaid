package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.ShareInfo
import com.chilisaft.undroid.data.models.WidgetResult
import com.chilisaft.undroid.graphql.SharesQuery
import javax.inject.Inject

/** Backs the Shares screen - a read-only list of user shares (needs the API key's SHARE permission). */
class SharesRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getShares(): WidgetResult<List<ShareInfo>> =
        apolloClient.runWidgetQuery(SharesQuery()) { data ->
            data.shares.map { it.toShareInfo() }.sortedBy { it.name.lowercase() }
        }

    private fun SharesQuery.Share.toShareInfo() = ShareInfo(
        id = id,
        name = name ?: "Unnamed share",
        comment = comment?.takeIf { it.isNotBlank() },
        freeKb = free?.toLongOrNull(),
        sizeKb = size?.toLongOrNull()?.takeIf { it > 0 }
    )
}
