package com.batuhan.reposwipe.feature.auth.data

import com.batuhan.reposwipe.core.datastore.TokenDataStore
import com.google.firebase.auth.FirebaseAuth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// githubProvider() itself isn't covered here: OAuthProvider.Builder.build() populates a real
// android.os.Bundle internally, which throws "not mocked" in a plain JVM unit test (this project
// has no Robolectric/instrumented suite — see CLAUDE.md) the moment it's invoked for real, and the
// built OAuthProvider doesn't expose its scopes back for assertion anyway (only getProviderId()).
class AuthRepositoryImplTest {
    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val tokenDataStore = mockk<TokenDataStore>()

    @Before
    fun setUp() {
        // isAuthenticated reads this eagerly at construction time, so every repository() call
        // needs it stubbed even in tests that aren't exercising isAuthenticated themselves.
        every { tokenDataStore.accessToken } returns flowOf(null)
    }

    private fun repository() = AuthRepositoryImpl(firebaseAuth, tokenDataStore)

    @Test
    fun `signOut clears the stored token and signs out of Firebase`() =
        runBlocking {
            coEvery { tokenDataStore.clearAccessToken() } returns Unit

            repository().signOut()

            coVerify { tokenDataStore.clearAccessToken() }
            verify { firebaseAuth.signOut() }
        }

    @Test
    fun `isAuthenticated is false when no token is stored`() =
        runBlocking {
            every { tokenDataStore.accessToken } returns flowOf(null)

            assertTrue(!repository().isAuthenticated.first())
        }

    @Test
    fun `isAuthenticated is true when a token is stored`() =
        runBlocking {
            every { tokenDataStore.accessToken } returns flowOf("ghu_token")

            assertTrue(repository().isAuthenticated.first())
        }

    @Test
    fun `saveToken delegates to the token data store`() =
        runBlocking {
            coEvery { tokenDataStore.saveAccessToken("ghu_token") } returns Unit

            repository().saveToken("ghu_token")

            coVerify { tokenDataStore.saveAccessToken("ghu_token") }
        }
}
