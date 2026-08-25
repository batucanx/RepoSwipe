package com.batuhan.reposwipe.feature.auth.onboarding

enum class OnboardingStep { LANGUAGE, TOPIC, POPULARITY }

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val selectedLanguage: String? = null,
    val selectedTopic: String? = null,
    val finished: Boolean = false,
)
