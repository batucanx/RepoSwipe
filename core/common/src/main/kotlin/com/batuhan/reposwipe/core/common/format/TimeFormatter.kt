package com.batuhan.reposwipe.core.common.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** ISO-8601 timestamp (as returned by the GitHub API) -> "2h ago" style relative label. */
fun String.toRelativeTimeLabel(): String {
    val instant = runCatching { Instant.parse(this) }.getOrNull() ?: return this
    val minutes = ChronoUnit.MINUTES.between(instant, Instant.now())
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

/** Unix epoch seconds (e.g. GitHub's `x-ratelimit-reset`) -> local "HH:mm" clock label. */
fun Long.toClockTimeLabel(): String {
    val time = Instant.ofEpochSecond(this).atZone(ZoneId.systemDefault())
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}
