package com.batuhan.reposwipe.feature.settings

import com.batuhan.reposwipe.core.common.theme.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
