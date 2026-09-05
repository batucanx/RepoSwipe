# RepoSwipe

[![Android CI](https://github.com/batucanx/RepoSwipe/actions/workflows/android-ci.yml/badge.svg)](https://github.com/batucanx/RepoSwipe/actions/workflows/android-ci.yml)

A Tinder-style Android app for discovering GitHub repositories — swipe right to star, left to pass. Built as a production-shaped portfolio project: real GitHub API data, offline-first star syncing, a live global "most-starred-today" leaderboard, and hand-built swipe physics. UI follows a dark-only, glassmorphic lime-accent design system across every screen.

## Features

- **Discover** — swipe through trending repos by language, backed by GitHub's Search API and cached with Room + Paging 3
- **Custom swipe deck** — real release-velocity tracking (a fast flick commits under the drag threshold, same as native fling gestures), spring physics for both the exit and the cancelled-drag snap-back, and a smoothly animated hand-off as the next card advances to front
- **Trending Today** — a live, cross-device leaderboard of the day's most-starred repos, aggregated in Firebase Firestore
- **Starred** — your starred repos with language filters, kept in sync via an offline outbox (WorkManager) so starring/unstarring works offline and catches up later
- **Profile** — GitHub account stats, recent repositories, and sign-out
- **GitHub sign-in via Firebase Authentication** — no password entry in-app; the GitHub access token is stored encrypted afterward (DataStore + Android Keystore/Tink)
- Crash and performance monitoring via Sentry
- [Privacy policy](docs/index.html), published via GitHub Pages

## Tech stack

- **UI**: Jetpack Compose, Material 3, a custom hand-built swipe deck (no third-party swipe library), dark-only glassmorphic design system (Plus Jakarta Sans)
- **Architecture**: MVI + Clean Architecture, multi-module Gradle (Now-in-Android style)
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp + kotlinx.serialization
- **Persistence**: Room, Paging 3, DataStore, Android Keystore (Tink)
- **Backend**: Firebase Firestore (leaderboard aggregation)
- **Async**: Kotlin Coroutines + Flow
- **Observability**: Sentry (crash reporting + performance tracing)
- **Security**: Firebase App Check (Play Integrity in release builds, debug provider in debug builds) attests that Firestore/Auth calls come from a genuine build of this app
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
:feature:auth                # GitHub sign-in via Firebase Auth
:feature:swipe                # Discover screen + swipe deck
:feature:filter               # Language/topic filters
:feature:leaderboard          # Trending Today
:feature:starred              # My Stars
:feature:profile              # Account + sign out
```

## Getting started

### Prerequisites

- Android Studio (Koala or newer) / JDK 17
- A Firebase project with a GitHub OAuth App wired up as a sign-in provider — **required**, the app can't get past the sign-in screen without it. Create the OAuth App at [github.com/settings/developers](https://github.com/settings/developers), then in the Firebase Console go to Authentication → Sign-in method → add **GitHub**, paste that App's Client ID/Secret, and copy the callback URL Firebase generates there back into the OAuth App's own "Authorization callback URL"
- Optional: Firestore enabled on that same Firebase project (for the leaderboard) and a Sentry project (for crash reporting) — the app runs without either, those two features just no-op
- Optional (debug builds only): a Firebase App Check debug token, registered under App Check → Apps in the Firebase Console. Debug builds print an unregistered-token warning to Logcat on first run with the token to register; without registering it, App Check-enforced Firebase calls (once enforcement is turned on) will fail on that device

### Setup

1. Clone the repo
2. Create `local.properties` in the project root (gitignored):
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   sentry.dsn=https://...@....ingest.sentry.io/...   # optional
   sentry.org=...           # optional — enables ProGuard mapping upload on release builds
   sentry.project=...       # optional
   sentry.authToken=...     # optional — mapping upload is skipped (not a build failure) if unset
   ```
3. Drop your Firebase project's `google-services.json` into `app/` — required even before Firestore/leaderboard is wired up, since the Gradle plugin needs *a* file present just to build
4. Build: `./gradlew build`, or open in Android Studio and run

### Firestore security rules

If you also enable Firestore on that Firebase project, the leaderboard needs these rules (Firestore Console → Rules). `LeaderboardRepository` never reads `request.auth` (Firebase Auth exists in this app for GitHub sign-in, but that's unrelated to these writes), so they constrain the *shape* of every write instead — scoped to just the leaderboard collection:

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
