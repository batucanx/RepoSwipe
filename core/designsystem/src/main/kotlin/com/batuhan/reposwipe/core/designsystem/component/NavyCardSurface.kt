package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.CardBackgroundNavy

/**
 * The fixed "Parisian night" navy background + hairline border shared by every card/row that uses
 * [CardBackgroundNavy] instead of the theme-driven `surfaceContainer` glass treatment — [RepoCard],
 * [RepoListItem], [UserListItem], [UserProfileHeader], and Profile's stat strip. Does not clip:
 * callers clip to their own shape (or not) independently, since some need it for ripple bounds and
 * some don't.
 */
fun Modifier.navyCardSurface(shape: Shape): Modifier =
    this
        .background(CardBackgroundNavy.copy(alpha = 0.96f), shape)
        .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
