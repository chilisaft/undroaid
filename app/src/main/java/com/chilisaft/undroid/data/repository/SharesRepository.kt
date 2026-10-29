package com.chilisaft.undroid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroid.data.api.runWidgetQuery
import com.chilisaft.undroid.data.models.ShareInfo
import com.chilisaft.undroid.data.models.ShareProtection
import com.chilisaft.undroid.data.models.ShareSettings
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
        sizeKb = size?.toLongOrNull()?.takeIf { it > 0 },
        protection = color.toShareProtection(),
        settings = ShareSettings(
            allocator = allocator?.takeIf { it.isNotBlank() },
            splitLevel = splitLevel?.trim(),
            minimumFreeKb = floor?.toLongOrNull(),
            includedDisks = include.orEmpty().filter { it.isNotBlank() },
            excludedDisks = exclude.orEmpty().filter { it.isNotBlank() }
        )
    )

    /** "green-on" was the only value seen on a real server; yellow/red follow Unraid's webUI. */
    private fun String?.toShareProtection(): ShareProtection? = when (this?.substringBefore('-')) {
        "green" -> ShareProtection.PROTECTED
        "yellow", "red" -> ShareProtection.UNPROTECTED
        else -> null
    }
}
