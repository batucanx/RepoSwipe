package com.batuhan.reposwipe.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.theme.ThemeMode
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

private const val GITHUB_REPO_URL = "https://github.com/batucanx/RepoSwipe"

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        SettingsTopBar(onClose = onClose)

        LazyColumn(
            contentPadding = PaddingValues(RepoSwipeTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xl),
        ) {
            item {
                AppearanceSection(
                    themeMode = uiState.themeMode,
                    onThemeModeChange = viewModel::setThemeMode,
                )
            }
            item { AccountSection(onSignOut = viewModel::signOut) }
            item { AboutSection() }
        }
    }
}

@Composable
private fun SettingsTopBar(onClose: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = RepoSwipeTheme.spacing.xs, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = RepoSwipeIcons.Close,
                contentDescription = stringResource(R.string.settings_close_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = RepoSwipeTheme.typography.displaySmMobile,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        Row(horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
            RepoSwipeFilterChip(
                label = stringResource(R.string.settings_theme_system),
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
            )
            RepoSwipeFilterChip(
                label = stringResource(R.string.settings_theme_light),
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
            )
            RepoSwipeFilterChip(
                label = stringResource(R.string.settings_theme_dark),
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) },
            )
        }
    }
}

@Composable
private fun AccountSection(onSignOut: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_section_account)) {
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = RepoSwipeIcons.SignOut,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.xs))
            Text(text = stringResource(R.string.settings_sign_out))
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName =
        remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull().orEmpty()
        }

    SettingsSection(title = stringResource(R.string.settings_section_about)) {
        Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md)) {
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL)))
                        }.padding(RepoSwipeTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_view_source),
                    style = RepoSwipeTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = RepoSwipeIcons.OpenExternal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md)) {
        Text(
            text = title.uppercase(),
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
