# RepoSwipe

Tinder-style GitHub repo discovery Android app (portfolio project). Swipe right = star, left = pass; trending/filter/starred/profile screens.

## Stack

- MVI + Clean Architecture, multi-module Gradle (Now-in-Android style): `:app`, `:core:{common,network,database,datastore,designsystem,data}`, `:feature:{auth,swipe,filter,leaderboard,starred,profile}`
- Jetpack Compose, custom hand-built swipe deck (no third-party swipe library)
- Hilt DI, Retrofit + OkHttp + Room + Paging3, StateFlow/SharedFlow
- GitHub Device Flow auth, DataStore + Keystore/Tink for token storage
- Coil for images
- namespace `com.batuhan.reposwipe`, minSdk 26 / targetSdk 35 / compileSdk 35, Kotlin/JVM 17
- `ksp.useKSP2=false` — Hilt 2.52 + KSP2(K2) breaks `@AndroidEntryPoint`; stay on classic KSP1
- Project lives at `C:\Project\GithubProject` (moved 2026-07-20 from `...\Masaüstü\KotlinLearn` — that path had a non-ASCII AGP issue, worked around with `android.overridePathCheck=true`; the flag is removed now that the path is ASCII-only)

## Design source of truth

Real mockups + design tokens live at `C:\Project\Github_Project\dark_themes\` and `ligh_themes\` (`screen.png` + `code.html` per screen, plus `DESIGN.md` with M3 color/typography/spacing tokens). Match these closely, don't invent a new look. `code.html` also encodes exact swipe drag physics (150px commit threshold, rotation = dx/20, spring-back `cubic-bezier(0.175,0.885,0.32,1.275)`).

## Plan

Phased build plan: `C:\Users\batuh\.claude\plans\sen-uzman-bir-senior-zesty-hoare.md` — check current phase before resuming work. **All 10 phases done as of 2026-07-21**, including `feature:filter`'s `FilterScreen` (found as a leftover Faz 0 placeholder, built out afterward per the `discovery_filters` mockup — language/topic multi-select, min-star slider, "updated this week").

Discover filters (`DiscoverFilters` in `core:data/model`) are shared through a `@Singleton DiscoverFilterRepository` (in-memory `StateFlow`, no persistence) so the Discover screen's own quick-filter chip row and the dedicated Filter screen stay in sync — toggling a language from either place updates the same state. `RepoRepository.buildQuery` maps it onto GitHub's search qualifiers: `language:`/`topic:` OR-grouped within each field, `stars:>=`, `pushed:>` for "updated this week". The mockup's second "with documentation" checkbox was dropped — no GitHub Search API qualifier can honestly back it.

`SwipeDeck` (`core/designsystem/.../SwipeDeck.kt`) tracks real release velocity (`VelocityTracker`) so a fast flick commits under the 150dp threshold and both the exit-fling and the cancelled-drag spring-back inherit that velocity. The next card's promotion to front (scale/offset/alpha) animates via `animateFloatAsState` instead of hard-cutting — the front/background Box is a single unified composable (conditional `.then()` for pointerInput/semantics only) so state survives the front/background transition. `SwipeActionButton` got a spring press-scale (0.9x) tied to `MutableInteractionSource`.

Sentry DSN lives in the gitignored root `local.properties` as `sentry.dsn=...`, wired into `BuildConfig.SENTRY_DSN` (`app/build.gradle.kts`, same pattern as `github.clientId`) and read in `RepoSwipeApp.onCreate()`. `tracesSampleRate = 0.2`. **Important:** the Sentry SDK's own auto-init `ContentProvider` must stay disabled via `<meta-data android:name="io.sentry.auto-init" android:value="false" />` in `AndroidManifest.xml` — without it the app crashes on launch with "DSN is required" because that provider runs before `RepoSwipeApp.onCreate()` and doesn't know about `BuildConfig.SENTRY_DSN`.

`EmptyState` (`core/designsystem/.../component/EmptyState.kt`) is the shared empty-results/error composable used across Swipe, Starred, Leaderboard, and Profile — icon + title + message + optional retry action.

detekt + ktlint are applied to every module via a `subprojects {}` block in the root `build.gradle.kts` (cross-cutting concern, unlike feature deps which stay per-module). Config: `config/detekt/detekt.yml` (MagicNumber/TooManyFunctions/MatchingDeclarationName off, LongMethod/CyclomaticComplexMethod/LongParameterList/ReturnCount thresholds raised — all tuned for idiomatic Compose UI code, not blanket-disabled) and root `.editorconfig` (`ktlint_function_naming_ignore_when_annotated_with = Composable`, `ktlint_standard_property-naming = disabled` for `_foo`/`foo` StateFlow backing properties). Both wired into `check`, so `./gradlew build` already enforces them — matches the existing single-command verification convention. `./gradlew ktlintFormat` auto-fixes formatting.

GitHub Actions CI (`.github/workflows/android-ci.yml`) runs `./gradlew build` on push/PR to `main`; it stubs a fake `app/google-services.json` first since the real one is gitignored and Firebase's Gradle plugin hard-fails without *a* file present (the stub isn't a real credential, just satisfies the file-exists check).

## Repo

Pushed to `https://github.com/batucanx/RepoSwipe` (branch `main`). Git identity for this repo is set locally (not global): `user.name=batucanx`, `user.email=batucanx@users.noreply.github.com`. On a fresh clone, recreate the gitignored `local.properties` with `sdk.dir=...` and `github.clientId=...` before building/running auth.

## Working agreements

- No automated test suite — user tests manually. Just write code and verify it builds (`./gradlew build` or module-level compile).
- For external service setup (Firebase, Sentry, etc.) pause and let the user do the console/dashboard steps themselves.
