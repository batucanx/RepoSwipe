package com.batuhan.reposwipe.core.designsystem.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.batuhan.reposwipe.core.common.text.UiText

@Composable
@Suppress("SpreadOperator") // bridging UiText's List<Any> to stringResource's vararg; args lists are always tiny
fun UiText.asString(): String =
    when (this) {
        is UiText.Dynamic -> value
        is UiText.Resource -> stringResource(resId, *args.toTypedArray())
    }
