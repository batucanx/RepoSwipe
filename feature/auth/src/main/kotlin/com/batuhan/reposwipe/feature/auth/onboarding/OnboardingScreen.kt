package com.batuhan.reposwipe.feature.auth.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.data.model.AvailableLanguages
import com.batuhan.reposwipe.core.data.model.AvailableTopics
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import com.batuhan.reposwipe.feature.auth.R

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onFinished()
    }

    if (uiState.isLoading || uiState.finished) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(RepoSwipeTheme.spacing.xl),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.finish(popularOnly = false) }) {
                Text(
                    text = stringResource(R.string.onboarding_skip_all),
                    style = RepoSwipeTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        StepProgress(step = uiState.step, modifier = Modifier.padding(bottom = RepoSwipeTheme.spacing.xl))

        when (uiState.step) {
            OnboardingStep.LANGUAGE ->
                OptionStep(
                    title = stringResource(R.string.onboarding_language_title),
                    subtitle = stringResource(R.string.onboarding_language_subtitle),
                ) {
                    LanguageOptions(onSelect = viewModel::selectLanguage)
                    SkipStepButton(onClick = viewModel::skipLanguage)
                }
            OnboardingStep.TOPIC ->
                OptionStep(
                    title = stringResource(R.string.onboarding_topic_title),
                    subtitle = stringResource(R.string.onboarding_topic_subtitle),
                ) {
                    TopicOptions(onSelect = viewModel::selectTopic)
                    SkipStepButton(onClick = viewModel::skipTopic)
                }
            OnboardingStep.POPULARITY ->
                OptionStep(
                    title = stringResource(R.string.onboarding_popularity_title),
                    subtitle = stringResource(R.string.onboarding_popularity_subtitle),
                ) {
                    PopularityOptions(onFinish = viewModel::finish)
                }
        }
    }
}

@Composable
private fun StepProgress(
    step: OnboardingStep,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
        OnboardingStep.entries.forEach { entry ->
            val isCurrentOrPast = entry.ordinal <= step.ordinal
            Box(
                modifier =
                    Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentOrPast) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            CircleShape,
                        ),
            )
        }
    }
}

@Composable
private fun OptionStep(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
            Text(
                text = title,
                style = RepoSwipeTheme.typography.headlineLgMobile,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = subtitle,
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageOptions(onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
    ) {
        AvailableLanguages.forEach { language ->
            RepoSwipeFilterChip(
                label = language,
                selected = false,
                outlined = true,
                leadingDotColor = languageColor(language),
                onClick = { onSelect(language) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicOptions(onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
    ) {
        AvailableTopics.forEach { topic ->
            RepoSwipeFilterChip(
                label = topic.label,
                selected = false,
                outlined = true,
                onClick = { onSelect(topic.slug) },
            )
        }
    }
}

@Composable
private fun SkipStepButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onClick) {
            Text(
                text = stringResource(R.string.onboarding_skip_step),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PopularityOptions(onFinish: (popularOnly: Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm)) {
        PopularityOptionRow(
            label = stringResource(R.string.onboarding_popularity_popular),
            onClick = { onFinish(true) },
        )
        PopularityOptionRow(
            label = stringResource(R.string.onboarding_popularity_all),
            onClick = { onFinish(false) },
        )
    }
}

@Composable
private fun PopularityOptionRow(
    label: String,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), shape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
                .clickable(onClick = onClick)
                .padding(RepoSwipeTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = RepoSwipeIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}
