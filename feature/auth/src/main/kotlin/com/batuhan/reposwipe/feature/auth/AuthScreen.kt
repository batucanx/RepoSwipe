package com.batuhan.reposwipe.feature.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.designsystem.component.BrandWordmark
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

private const val BRAND_LOGO_HEIGHT_DP = 32

/**
 * Sign-in via Firebase Authentication's generic GitHub OAuth provider: [SignInContent]'s button
 * calls [FirebaseAuth.startActivityForSignInWithProvider][com.google.firebase.auth.FirebaseAuth],
 * which drives GitHub's own "Authorize RepoSwipe" browser flow internally (a single tap for anyone
 * already signed into github.com) and hands back an
 * [AuthResult][com.google.firebase.auth.AuthResult] on completion — [AuthViewModel] pulls GitHub's
 * own access token out of that and saves it. Replaces an earlier Device Flow screen (type an
 * 8-character code into a separate web page) — GitHub's own device-activation page requires that
 * manual entry by design and has no pre-fill/paste-friendly shortcut, which read as clunky enough
 * to move off entirely.
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Compose's LocalContext inside content set via ComponentActivity.setContent is that Activity
    // itself — startActivityForSignInWithProvider needs one (it can't be held by the ViewModel).
    val activity = LocalContext.current as Activity
    // Guards against a double-tap firing two concurrent sign-in flows: onSignInStarted()'s state
    // change only swaps the button out on the next recomposition, not synchronously with the
    // click, leaving a brief window where a second tap would otherwise start a second flow.
    var signInInFlight by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onAuthenticated()
        if (uiState != AuthUiState.SigningIn) signInInFlight = false
    }

    // Covers this screen surviving an Activity recreation (config change, or the process was
    // killed) while GitHub's sign-in page was in front — Firebase keeps the in-progress result
    // around for exactly this case instead of losing it.
    LaunchedEffect(Unit) {
        viewModel.firebaseAuth.pendingAuthResult
            ?.addOnSuccessListener { viewModel.onSignInResult(Result.success(it)) }
            ?.addOnFailureListener { viewModel.onSignInResult(Result.failure(it)) }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = RepoSwipeTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandWordmark(
            contentDescription = stringResource(R.string.auth_brand_name),
            modifier = Modifier.height(BRAND_LOGO_HEIGHT_DP.dp),
        )

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.xl))

        when (val state = uiState) {
            AuthUiState.SignedOut ->
                SignInContent(
                    onSignInClick = {
                        if (!signInInFlight) {
                            signInInFlight = true
                            viewModel.onSignInStarted()
                            viewModel.firebaseAuth
                                .startActivityForSignInWithProvider(activity, viewModel.githubProvider())
                                .addOnSuccessListener { viewModel.onSignInResult(Result.success(it)) }
                                .addOnFailureListener { viewModel.onSignInResult(Result.failure(it)) }
                        }
                    },
                )
            AuthUiState.SigningIn -> LoadingContent()
            AuthUiState.Success -> LoadingContent(message = stringResource(R.string.auth_success_redirecting))
            is AuthUiState.Error -> ErrorContent(state, onRetry = viewModel::retry)
        }
    }
}

@Composable
private fun SignInContent(onSignInClick: () -> Unit) {
    Text(
        text = stringResource(R.string.auth_sign_in_prompt),
        style = RepoSwipeTheme.typography.bodyLg,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.lg))

    Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.auth_continue_with_github))
    }
}

@Composable
private fun LoadingContent(message: String = stringResource(R.string.auth_connecting)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))
        Text(
            text = message,
            style = RepoSwipeTheme.typography.bodyLg,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(
    state: AuthUiState.Error,
    onRetry: () -> Unit,
) {
    Text(
        text = state.message.asString(),
        style = RepoSwipeTheme.typography.bodyLg,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))
    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.auth_action_retry))
    }
}
