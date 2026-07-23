package com.batuhan.reposwipe.core.common.text

/**
 * A UI string that isn't known until runtime (e.g. an error message decided in a ViewModel)
 * but still needs to resolve to a localized Android string resource rather than a hardcoded
 * literal. ViewModels build [Resource] values referencing their module's own `R.string.*`;
 * the Composable layer resolves them with `asString()` (in `core:designsystem`), which is the
 * only place that actually needs a `Context`/composition.
 */
sealed interface UiText {
    data class Dynamic(
        val value: String,
    ) : UiText

    data class Resource(
        val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}
