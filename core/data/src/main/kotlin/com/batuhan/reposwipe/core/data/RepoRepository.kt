package com.batuhan.reposwipe.core.data

import androidx.annotation.VisibleForTesting
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.batuhan.reposwipe.core.data.mapper.toDomain
import com.batuhan.reposwipe.core.data.model.Contributor
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.network.GitHubApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

interface RepoRepository {
    fun searchRepos(filters: DiscoverFilters): Flow<PagingData<Repo>>

    /** Free-text search (e.g. the dedicated Search screen) — [freeText] is prepended to the same
     * qualifiers [searchRepos] builds from [filters], so results still respect the star floor and
     * archived/language/topic constraints if any are passed. */
    fun searchRepos(
        freeText: String,
        filters: DiscoverFilters = DiscoverFilters(),
    ): Flow<PagingData<Repo>>

    /** Null success means the repo has no README file — a normal state, not an error. The
     * success value is GitHub's own already-rendered README HTML (see [GitHubApiService.getReadme]),
     * ready to load into a WebView as-is. */
    suspend fun getReadmeHtml(
        ownerLogin: String,
        name: String,
    ): Result<String?>

    /** Byte count per language, as GitHub's linguist reports it — larger byte count = more of the repo. */
    suspend fun getLanguageBreakdown(
        ownerLogin: String,
        name: String,
    ): Result<Map<String, Long>>

    suspend fun getContributors(
        ownerLogin: String,
        name: String,
    ): Result<List<Contributor>>

    /** Heuristic "similar repos" — GitHub has no native endpoint for this, so it searches by
     * [repo]'s own primary language + first topic, excluding [repo] itself. Empty success (not a
     * failure) when [repo] has neither a language nor a topic to search by. */
    suspend fun getSimilarRepos(repo: Repo): Result<List<Repo>>
}

class RepoRepositoryImpl
    @Inject
    constructor(
        private val api: GitHubApiService,
        private val database: AppDatabase,
    ) : RepoRepository {
        override fun searchRepos(filters: DiscoverFilters): Flow<PagingData<Repo>> = pagedSearch(buildQuery(filters))

        override fun searchRepos(
            freeText: String,
            filters: DiscoverFilters,
        ): Flow<PagingData<Repo>> = pagedSearch(buildQuery(filters, freeText))

        @OptIn(ExperimentalPagingApi::class)
        private fun pagedSearch(query: String): Flow<PagingData<Repo>> =
            Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                remoteMediator = RepoRemoteMediator(query = query, api = api, database = database),
                pagingSourceFactory = { database.repoDao().pagingSource(query) },
            ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

        /**
         * Runs on [Dispatchers.IO] — reading the response body plus the URL-rewriting regex pass
         * over the full README HTML are real CPU/IO work, and doing them on the caller's
         * dispatcher (typically `Main.immediate` from a ViewModel) froze the UI for large
         * READMEs. [CancellationException] is rethrown rather than turned into a [Result.failure]:
         * swallowing it here would let a job that [SwipeViewModel.loadReadme] already cancelled
         * (because the user opened a different repo) keep running and overwrite the new repo's
         * state with a stale result.
         */
        override suspend fun getReadmeHtml(
            ownerLogin: String,
            name: String,
        ): Result<String?> =
            withContext(Dispatchers.IO) {
                runCatching { api.getReadme(ownerLogin, name).string() }
                    .onFailure { if (it is CancellationException) throw it }
                    .recoverCatching { throwable ->
                        if (throwable is HttpException && throwable.code() == HTTP_NOT_FOUND) null else throw throwable
                    }.map { html -> html?.let { resolveRelativeReadmeUrls(it, ownerLogin, name) }?.let(::capReadmeLength) }
            }

        override suspend fun getLanguageBreakdown(
            ownerLogin: String,
            name: String,
        ): Result<Map<String, Long>> =
            runCatching { api.getLanguages(ownerLogin, name) }
                .onFailure { if (it is CancellationException) throw it }

        override suspend fun getContributors(
            ownerLogin: String,
            name: String,
        ): Result<List<Contributor>> =
            runCatching { api.getContributors(ownerLogin, name).map { it.toDomain() } }
                .onFailure { if (it is CancellationException) throw it }

        /**
         * A pure abuse/pathological-size backstop: [MAX_README_CHARS] only guards against a
         * multi-megabyte README ever reaching the WebView at all. The "Devamını gör" UX no longer
         * bounds render cost the way an earlier version did — `ReadmeWebView` now always renders
         * the whole document and merely *clips* it until expanded (see its doc for why: any
         * re-render on expand yanked the reader's scroll position). That parse/layout is the
         * WebView's own async work, off the main thread, so paying it up front is cheap; this cap
         * is what keeps "cheap" true for a pathological input. It's sized in HTML bytes, not
         * markdown text — GitHub's own rendered HTML runs several times larger than the source
         * markdown (per-heading anchor SVGs, class attributes, etc.), so this is deliberately much
         * larger than the old markdown-era cap was.
         */
        private fun capReadmeLength(text: String): String =
            if (text.length <= MAX_README_CHARS) {
                text
            } else {
                text.take(MAX_README_CHARS) + "\n\n…"
            }

        /**
         * GitHub's own rendered README HTML leaves `<img src>`/`<source srcset>`/`<a href>` paths
         * that are relative to the repo (e.g. `src="docs/banner.png"`) unresolved — verified
         * against the live API. `HEAD` is a valid git ref alias for "the default branch" on both
         * raw.githubusercontent.com and github.com/…/blob, so the repo's actual default branch
         * name never needs to be looked up. Images resolve against the *raw* content host (Coil-
         * style byte fetch); links resolve against the *blob* viewer (GitHub's own HTML page for a
         * file) since that's what a tap should open — these are different hosts, so `src`/`srcset`
         * and `href` are rewritten against different bases. Already-absolute/`data:`/`mailto:`/
         * `tel:`/fragment-only (`#…`) values are left untouched.
         */
        @VisibleForTesting
        internal fun resolveRelativeReadmeUrls(
            html: String,
            owner: String,
            repo: String,
        ): String {
            val rawBase = "https://raw.githubusercontent.com/$owner/$repo/HEAD/"
            val blobBase = "https://github.com/$owner/$repo/blob/HEAD/"
            return html
                .replace(srcsetAttrRegex) { match ->
                    val quote = match.groupValues[1]
                    val rewritten =
                        match.groupValues[2].split(",").joinToString(",") { entry ->
                            val trimmed = entry.trim()
                            if (trimmed.isEmpty()) {
                                entry
                            } else {
                                val parts = trimmed.split(whitespaceRunRegex, limit = 2)
                                val url = resolveReadmeAssetUrl(parts[0], rawBase)
                                if (parts.size > 1) "$url ${parts[1]}" else url
                            }
                        }
                    "srcset=$quote$rewritten$quote"
                }.replace(relativeUrlAttrRegex) { match ->
                    val (attr, quote, url) = match.destructured
                    val base = if (attr == "href") blobBase else rawBase
                    "$attr=$quote${resolveReadmeAssetUrl(url, base)}$quote"
                }
        }

        private fun resolveReadmeAssetUrl(
            url: String,
            base: String,
        ): String =
            if (url.isBlank() || absoluteOrSpecialUrlRegex.containsMatchIn(url)) {
                url
            } else {
                base + url.removePrefix("./").removePrefix("/")
            }

        /**
         * Discover shows the best-matching repos for the user's filters, sorted by stars — no
         * implicit "recently created" constraint. An earlier version always required
         * `created:>6 months ago` to approximate a "trending" feed (no official GitHub trending
         * endpoint exists), but pairing that with a language/topic filter routinely produced zero
         * or near-zero results (e.g. "Kotlin" — gaining even a modest star count within 6 months
         * of creation is the exception, not the rule, for most languages).
         */
        @VisibleForTesting
        internal fun buildQuery(
            filters: DiscoverFilters,
            freeText: String? = null,
        ): String {
            val minStars = maxOf(MIN_STARS_FLOOR, filters.minStars)
            val parts = mutableListOf<String>()

            // The free-text term has to come first — GitHub's search API treats leading bare
            // tokens as the relevance-matched term and later qualifiers as filters, not the other
            // way around.
            freeText?.trim()?.takeIf { it.isNotEmpty() }?.let { parts += it }
            parts += "stars:>=$minStars"
            parts += "archived:${filters.archived}"
            filters.language?.let { parts += "language:$it" }
            filters.topic?.let { parts += "topic:$it" }

            return parts.joinToString(" ")
        }

        /**
         * No official "similar repos" concept exists in GitHub's API, so this approximates one:
         * search by the repo's own primary language + first topic (its strongest two signals of
         * "what kind of project this is"), sorted by stars, then drop the repo itself from the
         * results client-side (no search qualifier can exclude a specific repo by id).
         */
        override suspend fun getSimilarRepos(repo: Repo): Result<List<Repo>> {
            val query = buildSimilarQuery(repo.language, repo.topics.firstOrNull()) ?: return Result.success(emptyList())
            return runCatching {
                api
                    .searchRepositories(query = query, page = 1, perPage = SIMILAR_REPOS_FETCH_COUNT)
                    .items
                    .asSequence()
                    .filterNot { it.id == repo.id }
                    .take(MAX_SIMILAR_REPOS)
                    .map { it.toDomain() }
                    .toList()
            }.onFailure { if (it is CancellationException) throw it }
        }

        @VisibleForTesting
        internal fun buildSimilarQuery(
            language: String?,
            topic: String?,
        ): String? {
            if (language == null && topic == null) return null
            val parts = mutableListOf("stars:>=$MIN_STARS_FLOOR", "archived:false")
            language?.let { parts += "language:$it" }
            topic?.let { parts += "topic:$it" }
            return parts.joinToString(" ")
        }

        @VisibleForTesting
        internal companion object {
            const val PAGE_SIZE = 20
            const val MIN_STARS_FLOOR = 50
            const val HTTP_NOT_FOUND = 404
            const val MAX_README_CHARS = 2_000_000
            const val MAX_SIMILAR_REPOS = 10

            // One extra beyond MAX_SIMILAR_REPOS so excluding the repo itself (if it happens to
            // be in this page) still leaves a full page's worth of results.
            const val SIMILAR_REPOS_FETCH_COUNT = MAX_SIMILAR_REPOS + 1

            private val whitespaceRunRegex = Regex("\\s+")

            // "src"/"href" (not "srcset", handled separately below since it can hold a
            // comma-separated list of URLs) attribute values on any tag; see
            // resolveRelativeReadmeUrls's doc for why src/href resolve against different bases.
            private val relativeUrlAttrRegex = Regex("""\b(src|href)=(["'])([^"']*)\2""")
            private val srcsetAttrRegex = Regex("""\bsrcset=(["'])([^"']*)\1""")

            // Anything already absolute, or a scheme this rewrite shouldn't touch (an in-page
            // "#heading" anchor, a "mailto:"/"tel:" link, an inline "data:" image).
            private val absoluteOrSpecialUrlRegex = Regex("""^(?:https?://|//|data:|mailto:|tel:|#)""", RegexOption.IGNORE_CASE)
        }
    }
