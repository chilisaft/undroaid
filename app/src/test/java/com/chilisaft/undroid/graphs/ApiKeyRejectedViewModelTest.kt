package com.chilisaft.undroid.graphs

import com.chilisaft.undroid.data.api.ApiKeyStatus
import com.chilisaft.undroid.data.repository.LoginRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ApiKeyRejectedViewModelTest {

    @Test
    fun `logInAgain forgets the key and clears the rejection`() {
        val apiKeyStatus = ApiKeyStatus().apply { reportRejected() }
        val loginRepository: LoginRepository = mockk { every { forgetApiKey() } returns Unit }
        val viewModel = ApiKeyRejectedViewModel(apiKeyStatus, loginRepository)
        assertThat(viewModel.keyRejected.value).isTrue()

        viewModel.logInAgain()

        verify { loginRepository.forgetApiKey() }
        assertThat(viewModel.keyRejected.value).isFalse()
    }
}
