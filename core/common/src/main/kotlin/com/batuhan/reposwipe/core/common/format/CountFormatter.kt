package com.batuhan.reposwipe.core.common.format

/** 1_234 -> "1.2k", 4_200_000 -> "4.2M". Matches the mockups' stat-badge style. */
fun Int.toCompactCount(): String =
    when {
        this >= 1_000_000 -> "%.1fM".format(this / 1_000_000f)
        this >= 1_000 -> "%.1fk".format(this / 1_000f)
        else -> toString()
    }
