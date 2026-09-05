package com.batuhan.reposwipe.feature.auth.data

import com.batuhan.reposwipe.core.datastore.TokenDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    /** [FirebaseAuth.startActivityForSignInWithProvider] needs an `Activity`, which this
     * Application-scoped repository must not hold onto — exposed so
     * [AuthScreen][com.batuhan.reposwipe.feature.auth.AuthScreen] can call it directly with its
     * own Activity context, matching Firebase's own documented usage pattern. */
    val firebaseAuth: FirebaseAuth

    /** GitHub as a generic Firebase Auth OAuth provider — Firebase's own backend holds the OAuth
     * App's client secret and does the code-for-token exchange, so this app never talks to
     * GitHub's token endpoint (or needs a client ID of its own) at all. */
    fun githubProvider(): OAuthProvider

    suspend fun saveToken(token: String)

    suspend fun signOut()
}

/**
 * GitHub sign-in via Firebase Authentication's generic OAuth provider support (2026-09-05),
 * replacing an earlier hand-rolled Authorization Code flow backed by a custom token-exchange
 * proxy: this project already runs a Firebase project (Firestore, for the Leaderboard) with
 * `google-services.json` set up, so routing sign-in through it needs no new external service —
 * the OAuth App's client secret is entered once in the Firebase console (Authentication > Sign-in
 * method > GitHub) instead of being held by app-owned infrastructure.
 *
 * [AuthScreen][com.batuhan.reposwipe.feature.auth.AuthScreen] drives the actual sign-in call
 * ([FirebaseAuth.startActivityForSignInWithProvider]) and hands the resulting
 * [com.google.firebase.auth.AuthResult] to
 * [AuthViewModel][com.batuhan.reposwipe.feature.auth.AuthViewModel], which pulls GitHub's own
 * access token out of it (`(authResult.credential as OAuthCredential).accessToken`) and calls
 * [saveToken]. This repository only ever stores that raw token, in the same [TokenDataStore] as
 * before — RepoSwipe's own GitHub REST API calls use it directly via `AuthInterceptor` and have no
 * idea Firebase was ever involved; Firebase's own session (`FirebaseUser`) is a separate, parallel
 * concern this app doesn't otherwise use, kept in sync only via [signOut].
 */
class AuthRepositoryImpl
    @Inject
    constructor(
        override val firebaseAuth: FirebaseAuth,
        private val tokenDataStore: TokenDataStore,
    ) : AuthRepository {
        override val isAuthenticated: Flow<Boolean> =
            tokenDataStore.accessToken.map { !it.isNullOrBlank() }

        override fun githubProvider(): OAuthProvider =
            OAuthProvider
                .newBuilder(GITHUB_PROVIDER_ID)
                .apply {
                    // public_repo: star/unstar public repos. user:follow: follow/unfollow users —
                    // a token issued before this scope existed won't have it; signing out and
                    // back in re-authorizes with the current scope list.
                    scopes = listOf("public_repo", "user:follow")
                }.build()

        override suspend fun saveToken(token: String) = tokenDataStore.saveAccessToken(token)

        override suspend fun signOut() {
            tokenDataStore.clearAccessToken()
            firebaseAuth.signOut()
        }

        private companion object {
            const val GITHUB_PROVIDER_ID = "github.com"
        }
    }
