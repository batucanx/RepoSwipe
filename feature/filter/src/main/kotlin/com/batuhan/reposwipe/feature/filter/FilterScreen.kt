package com.batuhan.reposwipe.feature.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import kotlin.math.roundToInt

@Composable
fun FilterScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        FilterTopBar(onClose = onClose, onReset = viewModel::reset)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(RepoSwipeTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xl),
        ) {
            item { FilterHeader() }
            item {
                LanguagesSection(
                    filters = uiState.filters,
                    onToggleLanguage = viewModel::toggleLanguage,
                )
            }
            item {
                TopicsSection(
                    filters = uiState.filters,
                    onToggleTopic = viewModel::toggleTopic,
                )
            }
            item {
                MinStarsSection(
                    filters = uiState.filters,
                    onMinStarsChange = viewModel::setMinStars,
                )
            }
            item {
                RepositoryStatusSection(
                    filters = uiState.filters,
                    onUpdatedRecentlyChange = viewModel::setUpdatedRecently,
                )
            }
        }

        ApplyFiltersButton(onClick = onClose)
    }
}

@Composable
private fun FilterTopBar(
    onClose: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = RepoSwipeTheme.spacing.xs, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = RepoSwipeIcons.Close,
                    contentDescription = "Kapat",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "RepoSwipe",
                style = RepoSwipeTheme.typography.displaySmMobile,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TextButton(onClick = onReset) {
            Text(
                text = "Reset",
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FilterHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base)) {
        Text(
            text = "Refine Discovery",
            style = RepoSwipeTheme.typography.headlineMd,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Adjust your preferences to find the perfect repositories for your next project.",
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagesSection(
    filters: DiscoverFilters,
    onToggleLanguage: (String) -> Unit,
) {
    FilterSection(title = "Programming Languages", trailing = "${filters.languages.size} Selected") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
            AvailableLanguages.forEach { language ->
                RepoSwipeFilterChip(
                    label = language,
                    selected = language in filters.languages,
                    outlined = true,
                    leadingDotColor = languageColor(language),
                    onClick = { onToggleLanguage(language) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicsSection(
    filters: DiscoverFilters,
    onToggleTopic: (String) -> Unit,
) {
    FilterSection(title = "Popular Topics") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
            AvailableTopics.forEach { topic ->
                RepoSwipeFilterChip(
                    label = topic.label,
                    selected = topic.slug in filters.topics,
                    outlined = true,
                    onClick = { onToggleTopic(topic.slug) },
                )
            }
        }
    }
}

@Composable
private fun MinStarsSection(
    filters: DiscoverFilters,
    onMinStarsChange: (Int) -> Unit,
) {
    val starLabel = if (filters.minStars <= MIN_STARS_FLOOR) "Any" else "${filters.minStars.toCompactCount()}+"
    FilterSection(title = "Minimum Stars", trailing = starLabel) {
        Column {
            Slider(
                value = filters.minStars.toFloat(),
                onValueChange = { onMinStarsChange(it.toNearestStep()) },
                valueRange = MIN_STARS_FLOOR.toFloat()..MAX_STARS.toFloat(),
                steps = (MAX_STARS / STAR_STEP) - 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("0", "10k", "25k", "50k+").forEach { label ->
                    Text(
                        text = label,
                        style = RepoSwipeTheme.typography.labelMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryStatusSection(
    filters: DiscoverFilters,
    onUpdatedRecentlyChange: (Boolean) -> Unit,
) {
    FilterSection(title = "Repository Status") {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                    .padding(RepoSwipeTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Updated this week",
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Checkbox(checked = filters.updatedRecently, onCheckedChange = onUpdatedRecentlyChange)
        }
    }
}

@Composable
private fun ApplyFiltersButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(RepoSwipeTheme.spacing.gutter)) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(text = "Apply Filters", style = RepoSwipeTheme.typography.headlineMd)
            Icon(
                imageVector = RepoSwipeIcons.ApplyFilled,
                contentDescription = null,
                modifier = Modifier.padding(start = RepoSwipeTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title.uppercase(),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = RepoSwipeTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        content()
    }
}

private fun Float.toNearestStep(): Int = (this / STAR_STEP).roundToInt() * STAR_STEP
