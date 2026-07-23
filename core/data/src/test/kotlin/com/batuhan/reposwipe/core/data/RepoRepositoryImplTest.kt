package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.network.GitHubApiService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RepoRepositoryImplTest {
    private val repository =
        RepoRepositoryImpl(
            api = mockk<GitHubApiService>(),
            database = mockk<AppDatabase>(),
        )

    @Test
    fun `default filters produce only the base created and stars qualifiers`() {
        val query = repository.buildQuery(DiscoverFilters())

        val sinceDate = LocalDate.now().minusMonths(6)
        assertEquals("created:>$sinceDate stars:>=${RepoRepositoryImpl.MIN_STARS_FLOOR}", query)
    }

    @Test
    fun `minStars below the floor is clamped up to the floor`() {
        val query = repository.buildQuery(DiscoverFilters(minStars = 1))

        assertTrue(query.contains("stars:>=${RepoRepositoryImpl.MIN_STARS_FLOOR}"))
    }

    @Test
    fun `minStars above the floor is used as-is`() {
        val query = repository.buildQuery(DiscoverFilters(minStars = 5_000))

        assertTrue(query.contains("stars:>=5000"))
    }

    @Test
    fun `languages are OR-grouped inside their own parentheses`() {
        val query = repository.buildQuery(DiscoverFilters(languages = setOf("Kotlin", "Rust")))

        assertTrue(
            query.contains("(language:Kotlin OR language:Rust)") ||
                query.contains("(language:Rust OR language:Kotlin)"),
        )
    }

    @Test
    fun `topics are OR-grouped inside their own parentheses`() {
        val query = repository.buildQuery(DiscoverFilters(topics = setOf("android", "compose")))

        assertTrue(
            query.contains("(topic:android OR topic:compose)") ||
                query.contains("(topic:compose OR topic:android)"),
        )
    }

    @Test
    fun `updatedRecently adds a pushed qualifier seven days back`() {
        val query = repository.buildQuery(DiscoverFilters(updatedRecently = true))

        val sinceDate = LocalDate.now().minusDays(7)
        assertTrue(query.contains("pushed:>$sinceDate"))
    }

    @Test
    fun `updatedRecently is omitted by default`() {
        val query = repository.buildQuery(DiscoverFilters())

        assertTrue(!query.contains("pushed:>"))
    }
}
