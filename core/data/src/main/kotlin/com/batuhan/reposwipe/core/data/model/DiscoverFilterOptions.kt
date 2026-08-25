package com.batuhan.reposwipe.core.data.model

/** A GitHub topic offered as a discover/onboarding filter option — [slug] is the actual
 * `topic:` search qualifier value, [label] is what's shown in the UI. */
data class FilterTopic(
    val label: String,
    val slug: String,
)

/** Shared between the Filter screen's language picker and the first-launch onboarding survey. */
val AvailableLanguages =
    listOf("JavaScript", "Python", "Rust", "Go", "Java", "TypeScript", "Kotlin", "Swift", "C++", "Ruby")

/** Shared between the Filter screen's topic picker and the first-launch onboarding survey. */
val AvailableTopics =
    listOf(
        FilterTopic("Machine Learning", "machine-learning"),
        FilterTopic("Web Development", "web-development"),
        FilterTopic("Mobile", "mobile"),
        FilterTopic("Blockchain", "blockchain"),
        FilterTopic("DevOps", "devops"),
        FilterTopic("Open Source", "open-source"),
    )
