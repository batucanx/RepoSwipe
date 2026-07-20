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

Phased build plan: `C:\Users\batuh\.claude\plans\sen-uzman-bir-senior-zesty-hoare.md` — check current phase before resuming work.

## Working agreements

- No automated test suite — user tests manually. Just write code and verify it builds (`./gradlew build` or module-level compile).
- For external service setup (Firebase, Sentry, etc.) pause and let the user do the console/dashboard steps themselves.
