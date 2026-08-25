package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/**
 * A single continuous pill track with an inset filled "thumb" on the selected option — used in
 * place of a row of separately-bordered [RepoSwipeFilterChip]s where the options are mutually
 * exclusive settings (e.g. theme mode) rather than a multi-select filter list.
 *
 * Generic rather than tied to any specific enum: designsystem components don't depend on
 * feature/core.common types, so callers resolve [optionLabel] strings (e.g. via `stringResource`)
 * before building the options list.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(percent = 50)
    val thumbShape = RoundedCornerShape(percent = 50)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(trackShape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), trackShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), trackShape)
                .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(thumbShape)
                        .then(
                            if (selected) {
                                Modifier.background(MaterialTheme.colorScheme.primaryContainer, thumbShape)
                            } else {
                                Modifier
                            },
                        ).clickable { onOptionSelected(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = optionLabel(option),
                    style = RepoSwipeTheme.typography.labelMd,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
