package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.network.GitHubApiService
import com.batuhan.reposwipe.core.network.model.ContributorDto
import com.batuhan.reposwipe.core.network.model.OwnerDto
import com.batuhan.reposwipe.core.network.model.RepoDto
import com.batuhan.reposwipe.core.network.model.SearchRepositoriesResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RepoRepositoryImplTest {
    private val api = mockk<GitHubApiService>()
    private val repository =
        RepoRepositoryImpl(
            api = api,
            database = mockk<AppDatabase>(),
        )

    @Test
    fun `default filters produce the base stars and archived qualifiers`() {
        val query = repository.buildQuery(DiscoverFilters())

        assertEquals("stars:>=${RepoRepositoryImpl.MIN_STARS_FLOOR} archived:false", query)
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
    fun `no minStars value ever adds a created-date qualifier`() {
        // A hardcoded "created recently" constraint used to pair with a high (or even moderate)
        // star floor and return zero results for most language/topic combinations — see
        // buildQuery's doc for why it was dropped entirely.
        listOf(0, 50, 5_000, 10_000).forEach { minStars ->
            val query = repository.buildQuery(DiscoverFilters(minStars = minStars))
            assertTrue("query for minStars=$minStars unexpectedly has a created: qualifier", !query.contains("created:>"))
        }
    }

    @Test
    fun `a selected language adds a single language qualifier`() {
        // Not OR-grouped: GitHub's repository search silently returns zero results for
        // "(language:A OR language:B)"-style queries, so only one language can ever be active.
        val query = repository.buildQuery(DiscoverFilters(language = "Kotlin"))

        assertTrue(query.contains("language:Kotlin"))
        assertTrue(!query.contains("OR"))
    }

    @Test
    fun `a selected topic adds a single topic qualifier`() {
        val query = repository.buildQuery(DiscoverFilters(topic = "android"))

        assertTrue(query.contains("topic:android"))
        assertTrue(!query.contains("OR"))
    }

    @Test
    fun `archived defaults to false (active repos only)`() {
        val query = repository.buildQuery(DiscoverFilters())

        assertTrue(query.contains("archived:false"))
    }

    @Test
    fun `archived true filters to archived repos only`() {
        val query = repository.buildQuery(DiscoverFilters(archived = true))

        assertTrue(query.contains("archived:true"))
    }

    @Test
    fun `getLanguageBreakdown returns the byte counts as-is on success`() =
        runTest {
            coEvery { api.getLanguages("batucanx", "reposwipe") } returns mapOf("Kotlin" to 50_000L, "Java" to 1_000L)

            val result = repository.getLanguageBreakdown("batucanx", "reposwipe")

            assertEquals(mapOf("Kotlin" to 50_000L, "Java" to 1_000L), result.getOrNull())
        }

    @Test
    fun `getLanguageBreakdown surfaces a network failure as Result failure`() =
        runTest {
            coEvery { api.getLanguages(any(), any()) } throws RuntimeException("offline")

            val result = repository.getLanguageBreakdown("batucanx", "reposwipe")

            assertTrue(result.isFailure)
        }

    @Test
    fun `getContributors maps DTOs to domain models`() =
        runTest {
            coEvery { api.getContributors("batucanx", "reposwipe", any()) } returns
                listOf(
                    ContributorDto(
                        login = "octocat",
                        avatarUrl = "https://avatars.example/octocat.png",
                        contributions = 42,
                        htmlUrl = "https://github.com/octocat",
                    ),
                )

            val result = repository.getContributors("batucanx", "reposwipe")

            val contributor = result.getOrNull()?.single()
            assertEquals("octocat", contributor?.login)
            assertEquals(42, contributor?.contributions)
        }

    @Test
    fun `getContributors surfaces a network failure as Result failure`() =
        runTest {
            coEvery { api.getContributors(any(), any(), any()) } throws RuntimeException("offline")

            val result = repository.getContributors("batucanx", "reposwipe")

            assertTrue(result.isFailure)
        }

    @Test
    fun `a free-text search term is prepended before the qualifiers`() {
        val query = repository.buildQuery(DiscoverFilters(), freeText = "react")

        assertEquals("react stars:>=${RepoRepositoryImpl.MIN_STARS_FLOOR} archived:false", query)
    }

    @Test
    fun `a blank free-text term is dropped instead of adding a stray leading space`() {
        val query = repository.buildQuery(DiscoverFilters(), freeText = "   ")

        assertEquals("stars:>=${RepoRepositoryImpl.MIN_STARS_FLOOR} archived:false", query)
    }

    @Test
    fun `buildSimilarQuery is null when the repo has neither a language nor a topic`() {
        assertNull(repository.buildSimilarQuery(language = null, topic = null))
    }

    @Test
    fun `buildSimilarQuery combines language and topic when both are present`() {
        val query = repository.buildSimilarQuery(language = "Kotlin", topic = "android")

        assertTrue(query!!.contains("language:Kotlin"))
        assertTrue(query.contains("topic:android"))
    }

    @Test
    fun `getSimilarRepos returns empty without a network call when the repo has no language or topic`() =
        runTest {
            val repo = testRepo(language = null, topics = emptyList())

            val result = repository.getSimilarRepos(repo)

            assertEquals(emptyList<Repo>(), result.getOrNull())
        }

    @Test
    fun `getSimilarRepos excludes the repo itself from the results`() =
        runTest {
            val repo = testRepo(id = 1L, language = "Kotlin", topics = emptyList())
            coEvery { api.searchRepositories(any(), any(), any(), any(), any()) } returns
                SearchRepositoriesResponseDto(
                    totalCount = 2,
                    incompleteResults = false,
                    items = listOf(repoDto(id = 1L), repoDto(id = 2L)),
                )

            val result = repository.getSimilarRepos(repo)

            assertEquals(listOf(2L), result.getOrNull()?.map { it.id })
        }

    private fun testRepo(
        id: Long = 1L,
        language: String? = "Kotlin",
        topics: List<String> = emptyList(),
    ) = Repo(
        id = id,
        name = "reposwipe",
        ownerLogin = "batucanx",
        ownerAvatarUrl = null,
        description = "",
        starCount = 100,
        forkCount = 1,
        language = language,
        updatedAt = "2026-01-01T00:00:00Z",
        htmlUrl = "https://github.com/batucanx/reposwipe",
        headerImageUrl = "",
        topics = topics,
    )

    private fun repoDto(id: Long) =
        RepoDto(
            id = id,
            name = "repo-$id",
            fullName = "owner/repo-$id",
            owner = OwnerDto(login = "owner"),
            stargazersCount = 100,
            forksCount = 1,
            updatedAt = "2026-01-01T00:00:00Z",
            htmlUrl = "https://github.com/owner/repo-$id",
        )

    @Test
    fun `getReadmeHtml returns null on a 404 (no README file)`() =
        runTest {
            coEvery { api.getReadme("batucanx", "reposwipe") } throws
                HttpException(Response.error<Any>(404, "".toResponseBody(null)))

            val result = repository.getReadmeHtml("batucanx", "reposwipe")

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun `getReadmeHtml returns GitHub's own rendered HTML on success`() =
        runTest {
            val html = """<div id="readme" class="md"><article class="markdown-body"><p>hi</p></article></div>"""
            coEvery { api.getReadme("batucanx", "reposwipe") } returns html.toResponseBody(null)

            val result = repository.getReadmeHtml("batucanx", "reposwipe")

            assertTrue(result.getOrNull()?.contains("<p>hi</p>") == true)
        }

    @Test
    fun `resolveRelativeReadmeUrls rewrites a relative img src against raw githubusercontent HEAD`() {
        val html = """<img src="docs/banner.png" alt="banner">"""

        val result = repository.resolveRelativeReadmeUrls(html, "owner", "repo")

        assertEquals(
            """<img src="https://raw.githubusercontent.com/owner/repo/HEAD/docs/banner.png" alt="banner">""",
            result,
        )
    }

    @Test
    fun `resolveRelativeReadmeUrls rewrites a relative a href against github blob HEAD`() {
        val html = """<a href="CONTRIBUTING.md">Contributing</a>"""

        val result = repository.resolveRelativeReadmeUrls(html, "owner", "repo")

        assertEquals(
            """<a href="https://github.com/owner/repo/blob/HEAD/CONTRIBUTING.md">Contributing</a>""",
            result,
        )
    }

    @Test
    fun `resolveRelativeReadmeUrls leaves absolute, data, mailto and fragment urls untouched`() {
        val html =
            """
            <img src="https://example.com/x.png">
            <img src="data:image/png;base64,AAAA">
            <a href="mailto:someone@example.com">mail</a>
            <a href="#user-content-heading">jump</a>
            """.trimIndent()

        val result = repository.resolveRelativeReadmeUrls(html, "owner", "repo")

        assertEquals(html, result)
    }

    @Test
    fun `resolveRelativeReadmeUrls rewrites a relative srcset entry, preserving its descriptor`() {
        val html = """<source srcset="dark.png 1x, ./assets/dark@2x.png 2x">"""

        val result = repository.resolveRelativeReadmeUrls(html, "owner", "repo")

        assertEquals(
            "<source srcset=\"https://raw.githubusercontent.com/owner/repo/HEAD/dark.png 1x," +
                "https://raw.githubusercontent.com/owner/repo/HEAD/assets/dark@2x.png 2x\">",
            result,
        )
    }
}
