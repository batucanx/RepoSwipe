package com.batuhan.reposwipe.feature.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.data.model.AvailableLanguages
import com.batuhan.reposwipe.core.data.model.AvailableTopics
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.designsystem.component.BrandWordmark
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import kotlin.math.roundToInt

private const val BRAND_LOGO_HEIGHT_DP = 32

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
            contentPadding = PaddingValues(RepoSwipeTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xl),
        ) {
            item { FilterHeader() }
            item {
                LanguagesSection(
                    filters = uiState.filters,
                    onSelectLanguage = viewModel::selectLanguage,
                )
            }
            item {
                TopicsSection(
                    filters = uiState.filters,
                    onSelectTopic = viewModel::selectTopic,
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
                    onArchivedChange = viewModel::setArchived,
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = RepoSwipeTheme.spacing.lg, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = RepoSwipeIcons.Close,
                    contentDescription = stringResource(R.string.filter_close_cd),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        BrandWordmark(
            contentDescription = stringResource(R.string.filter_brand_name),
            modifier = Modifier.height(BRAND_LOGO_HEIGHT_DP.dp),
        )
        TextButton(onClick = onReset) {
            Text(
                text = stringResource(R.string.filter_reset),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun FilterHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.filter_header_title),
            style = RepoSwipeTheme.typography.headlineLg,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.filter_header_subtitle),
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagesSection(
    filters: DiscoverFilters,
    onSelectLanguage: (String) -> Unit,
) {
    FilterSection(title = stringResource(R.string.filter_section_languages)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        ) {
            AvailableLanguages.forEach { language ->
                RepoSwipeFilterChip(
                    label = language,
                    selected = language == filters.language,
                    outlined = true,
                    leadingDotColor = languageColor(language),
                    onClick = { onSelectLanguage(language) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicsSection(
    filters: DiscoverFilters,
    onSelectTopic: (String) -> Unit,
) {
    FilterSection(title = stringResource(R.string.filter_section_topics)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        ) {
            AvailableTopics.forEach { topic ->
                RepoSwipeFilterChip(
                    label = topic.label,
                    selected = topic.slug == filters.topic,
                    outlined = true,
                    onClick = { onSelectTopic(topic.slug) },
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
    val starLabel =
        if (filters.minStars <= MIN_STARS_FLOOR) {
            stringResource(R.string.filter_stars_any)
        } else {
            stringResource(R.string.filter_stars_plus, filters.minStars.toCompactCount())
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), MaterialTheme.shapes.large)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), MaterialTheme.shapes.large)
                .padding(RepoSwipeTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.filter_section_min_stars).uppercase(),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = starLabel,
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
        }
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
                stringArrayResource(R.array.filter_stars_axis_labels).forEach { label ->
                    Text(
                        text = label,
                        style = RepoSwipeTheme.typography.labelMd,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryStatusSection(
    filters: DiscoverFilters,
    onArchivedChange: (Boolean) -> Unit,
) {
    FilterSection(title = stringResource(R.string.filter_section_repo_status)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        ) {
            RepoStatusOption(
                label = stringResource(R.string.filter_status_active),
                selected = !filters.archived,
                onClick = { onArchivedChange(false) },
                modifier = Modifier.weight(1f),
            )
            RepoStatusOption(
                label = stringResource(R.string.filter_status_archived),
                selected = filters.archived,
                onClick = { onArchivedChange(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RepoStatusOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (selected) 0.1f else 0.4f), shape)
                .border(
                    width = 1.dp,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        },
                    shape = shape,
                ).clip(shape = shape)
                .clickable(onClick = onClick)
                .padding(RepoSwipeTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RepoSwipeTheme.typography.bodySm,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        Icon(
            imageVector = if (selected) RepoSwipeIcons.ApplyFilled else RepoSwipeIcons.RadioUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun ApplyFiltersButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(RepoSwipeTheme.spacing.lg)) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(
                text = stringResource(R.string.filter_apply),
                style = RepoSwipeTheme.typography.headlineLgMobile,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Icon(
                imageVector = RepoSwipeIcons.ApplyFilled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = RepoSwipeTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md)) {
        Text(
            text = title.uppercase(),
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.secondary,
        )
        content()
    }
}

private fun Float.toNearestStep(): Int = (this / STAR_STEP).roundToInt() * STAR_STEP
