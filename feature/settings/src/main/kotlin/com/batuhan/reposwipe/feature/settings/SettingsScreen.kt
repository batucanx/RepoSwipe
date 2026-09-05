package com.batuhan.reposwipe.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.theme.ThemeMode
import com.batuhan.reposwipe.core.designsystem.component.DangerActionRow
import com.batuhan.reposwipe.core.designsystem.component.SegmentedControl
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.modifier.bottomHairline
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

private const val GITHUB_REPO_URL = "https://github.com/batucanx/RepoSwipe"

// Served via GitHub Pages from this repo's docs/index.html (Settings > Pages > Deploy from
// branch > main > /docs) — the URL Play Console's Data Safety form also links to.
private const val PRIVACY_POLICY_URL = "https://batucanx.github.io/RepoSwipe/"

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
                // Same visual system as RepoSwipeTopAppBar (used on every main tab) — translucent
                // surface + bottom hairline — even though this screen keeps its own close-icon-left
                // layout, which fits a modal/back-stack destination better than the tab bar's
                // menu+centered-title+trailing shape.
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .bottomHairline()
                .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.sm),
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
    val systemLabel = stringResource(R.string.settings_theme_system)
    val lightLabel = stringResource(R.string.settings_theme_light)
    val darkLabel = stringResource(R.string.settings_theme_dark)
    val labels =
        remember(systemLabel, lightLabel, darkLabel) {
            mapOf(
                ThemeMode.SYSTEM to systemLabel,
                ThemeMode.LIGHT to lightLabel,
                ThemeMode.DARK to darkLabel,
            )
        }

    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        SegmentedControl(
            options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            selectedOption = themeMode,
            optionLabel = { labels.getValue(it) },
            onOptionSelected = onThemeModeChange,
        )
    }
}

@Composable
private fun AccountSection(onSignOut: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_section_account)) {
        DangerActionRow(
            icon = RepoSwipeIcons.SignOut,
            label = stringResource(R.string.settings_sign_out),
            onClick = onSignOut,
        )
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
        // Version + the GitHub link as two rows in one shared container, rather than a bare Text
        // above a separately-boxed link row — balances against Account's now-lightweight row.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(RepoSwipeTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = RepoSwipeTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL)))
                        }.padding(RepoSwipeTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_view_source),
                        style = RepoSwipeTheme.typography.bodySm,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_view_source_subtitle),
                        style = RepoSwipeTheme.typography.labelMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = RepoSwipeIcons.OpenExternal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                        }.padding(RepoSwipeTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_policy),
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
