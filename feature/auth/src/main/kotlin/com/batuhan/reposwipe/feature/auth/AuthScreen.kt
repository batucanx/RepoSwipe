package com.batuhan.reposwipe.feature.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceFlowViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is DeviceFlowUiState.Success) onAuthenticated()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = RepoSwipeTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "RepoSwipe",
            style = RepoSwipeTheme.typography.displaySmMobile,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.xl))

        when (val state = uiState) {
            is DeviceFlowUiState.Loading -> LoadingContent()
            is DeviceFlowUiState.AwaitingUser -> AwaitingUserContent(state)
            is DeviceFlowUiState.Success -> LoadingContent(message = "Giriş yapıldı, yönlendiriliyorsun…")
            is DeviceFlowUiState.Error -> ErrorContent(state, onRetry = viewModel::retry)
        }
    }
}

@Composable
private fun LoadingContent(message: String = "GitHub'a bağlanılıyor…") {
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
private fun AwaitingUserContent(state: DeviceFlowUiState.AwaitingUser) {
    val context = LocalContext.current

    Text(
        text = "GitHub hesabınla giriş yap",
        style = RepoSwipeTheme.typography.bodyLg,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.lg))

    Text(
        text = state.userCode,
        style =
            RepoSwipeTheme.typography.displayLg.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
            ),
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.lg))

    Button(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.verificationUri)))
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "GitHub'da Aç")
    }

    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))

    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.sm))

    Text(
        text = "Kodu onayladığında otomatik olarak devam edeceğiz.",
        style = RepoSwipeTheme.typography.bodySm,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorContent(
    state: DeviceFlowUiState.Error,
    onRetry: () -> Unit,
) {
    Text(
        text = state.message,
        style = RepoSwipeTheme.typography.bodyLg,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))
    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(text = "Tekrar Dene")
    }
}
