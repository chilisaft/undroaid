package com.chilisaft.undroaid.data.api

import com.chilisaft.undroaid.utils.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

/**
 * ApolloClient.Builder validates transport configuration at construction time, not compile
 * time - conflicting builder calls (e.g. `webSocketServerUrl` alongside a manually-configured
 * `subscriptionNetworkTransport`) throw `IllegalStateException` only when actually built. This
 * previously crashed the app on launch (Hilt builds this eagerly for LoginRepository) even
 * though the module compiled cleanly, so this test constructs it for real rather than just
 * type-checking the wiring.
 */
class GraphQlServiceModuleTest {

    @Test
    fun `provideApolloClient builds without throwing`() {
        val storage: Storage = mockk {
            every { serverUrl } returns null
            every { apiToken } returns null
        }
        val okHttpClient = GraphQlServiceModule.provideOkHttpClient(UnraidInterceptor(storage))

        val apolloClient = GraphQlServiceModule.provideApolloClient(okHttpClient)

        assertThat(apolloClient).isNotNull()
    }
}
