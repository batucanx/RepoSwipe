# RepoSwipe

[![Android CI](https://github.com/batucanx/RepoSwipe/actions/workflows/android-ci.yml/badge.svg)](https://github.com/batucanx/RepoSwipe/actions/workflows/android-ci.yml)

A Tinder-style Android app for discovering GitHub repositories — swipe right to star, left to pass. Built as a production-shaped portfolio project: real GitHub API data, offline-first star syncing, a live global "most-starred-today" leaderboard, and hand-built swipe physics.

## Features

- **Discover** — swipe through trending repos by language, backed by GitHub's Search API and cached with Room + Paging 3
- **Custom swipe deck** — real release-velocity tracking (a fast flick commits under the drag threshold, same as native fling gestures), spring physics for both the exit and the cancelled-drag snap-back, and a smoothly animated hand-off as the next card advances to front
- **Trending Today** — a live, cross-device leaderboard of the day's most-starred repos, aggregated in Firebase Firestore
- **Starred** — your starred repos with language filters, kept in sync via an offline outbox (WorkManager) so starring/unstarring works offline and catches up later
- **Profile** — GitHub account stats and sign-out
- **GitHub Device Flow auth** — no password entry in-app; token stored encrypted (DataStore + Android Keystore/Tink)
- Crash and performance monitoring via Sentry

## Tech stack

- **UI**: Jetpack Compose, Material 3, a custom hand-built swipe deck (no third-party swipe library)
- **Architecture**: MVI + Clean Architecture, multi-module Gradle (Now-in-Android style)
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp + kotlinx.serialization
- **Persistence**: Room, Paging 3, DataStore, Android Keystore (Tink)
- **Backend**: Firebase Firestore (leaderboard aggregation)
- **Async**: Kotlin Coroutines + Flow
- **Observability**: Sentry (crash reporting + performance tracing)
- **Quality**: detekt (static analysis) + ktlint (formatting), enforced in CI

## Module structure

```
:app                        # Hilt/Compose Navigation shell, DI wiring
:core:common                # Shared utilities (formatting, auth contracts)
:core:designsystem          # Theme, typography, spacing, reusable components (SwipeDeck, RepoCard, ...)
:core:network                # Retrofit services, interceptors, DTOs
:core:database               # Room (repo cache, star outbox)
:core:datastore              # Encrypted token storage
:core:data                  # Repositories — the single source of truth per domain (repos, stars, leaderboard, user)
:feature:auth                # GitHub Device Flow
:feature:swipe                # Discover screen + swipe deck
:feature:filter               # Language/topic filters
:feature:leaderboard          # Trending Today
:feature:starred              # My Stars
:feature:profile              # Account + sign out
```

## Getting started

### Prerequisites

- Android Studio (Koala or newer) / JDK 17
- A GitHub OAuth App with Device Flow enabled ([create one](https://github.com/settings/developers) → New OAuth App → enable Device Flow)
- Optional: a Firebase project with Firestore (for the leaderboard) and a Sentry project (for crash reporting) — the app builds and runs without either, those features just no-op

### Setup

1. Clone the repo
2. Create `local.properties` in the project root (gitignored):
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   github.clientId=your_github_oauth_device_flow_client_id
   sentry.dsn=https://...@....ingest.sentry.io/...   # optional
   ```
3. (Optional, for the leaderboard) Drop your Firebase project's `google-services.json` into `app/`
4. Build: `./gradlew build`, or open in Android Studio and run

### Firestore security rules

If you wire up your own Firebase project, the leaderboard needs these rules (Firestore Console → Rules) since the app has no Firebase Authentication — writes are scoped to just the leaderboard collection:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /leaderboard/{date}/entries/{repoId} {
      allow read: if true;
      allow write: if request.resource.data.keys().hasAll(['repoId', 'repoName', 'ownerLogin', 'swipeCount'])
                   && request.resource.data.swipeCount is number;
    }
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## CI

Every push/PR to `main` runs `./gradlew build` — assembles debug + release, and enforces Android Lint, detekt, and ktlint. See [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml).

## Screenshots

_TBD — drop images in `docs/screenshots/` and reference them here._
