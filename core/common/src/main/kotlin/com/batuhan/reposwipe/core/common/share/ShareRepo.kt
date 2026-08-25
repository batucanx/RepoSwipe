package com.batuhan.reposwipe.core.common.share

import android.content.Intent

fun shareRepoIntent(htmlUrl: String): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, htmlUrl)
        },
        null,
    )
