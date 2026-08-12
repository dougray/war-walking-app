# WarWalker

A gamified wardriving fitness app: scan Wi-Fi/BLE with your phone, verify the walk with Health Connect step data, and upload the session straight to WiGLE. Android + Android Wear only.

This repo was scaffolded from a design conversation (originally drafted with Gemini) covering the architecture, DB schema, FastAPI backend, and Kotlin client. Claude reviewed that draft for correctness before turning it into a real, buildable project — see **Fixes applied** below for what was wrong in the original and why.

## License

Copyright (C) 2026 dougray. Licensed under the [GNU General Public License v3.0 or later](LICENSE) - meaning anyone can use, study, and modify this code, but any distributed derivative work must also be open-sourced under GPL-3.0. This is the only licensing action taken so far; there's no CONTRIBUTING guide or CLA yet.

## Architecture

The app talks to **WiGLE's API directly** - there is no backend server required to use it. This was a deliberate pivot away from an earlier design that proxied everything through a self-hosted FastAPI/Postgres backend (still present in `backend/`, see below).

- **Identity**: your WiGLE API Name/Token *is* the account - no separate WarWalker registration. Stored on-device with `EncryptedSharedPreferences` (`WigleCredentialStore.kt`).
- **Uploads**: the Android client posts the WigleWifi CSV straight to `https://api.wigle.net/api/v2/file/upload` (`network/WigleRepository.kt`).
- **Your live stats/rank**: pulled straight from `GET /api/v2/stats/user` and shown on the **Profile** tab - WiGLE already runs the global leaderboard, so this app doesn't need to build its own.
- **Everything WiGLE doesn't track** - steps, session scoring, streaks, and day-by-day history for charting - lives entirely on-device in a Room database (`data/`). WiGLE's stats API only gives you *current totals* and *this month vs. last month*, not a full history, so the "Last 7 Days" chart on the Profile tab is built from local data, not WiGLE's.

```
backend/            FastAPI + PostgreSQL - optional, not required to use the app (see below)
android/            Native Kotlin app (Jetpack Compose, Health Connect, Room, Retrofit)
```

## Opening the Android app

1. Open `android/` in Android Studio (SDK 37 / build-tools 36.0.0 already detected on this machine; AGP 9.3.0 needs Gradle 9.5.0 + JDK 17, both satisfied here).
2. Build and run. There's no server URL to configure - the app talks to `api.wigle.net` directly over HTTPS.
3. In the app's **Settings** tab, enter your WiGLE API Name and Token (see below for where to get them) and tap Verify & Save.

### Getting your WiGLE API credentials

Log into [wigle.net](https://wigle.net) normally, then go to your **Account** page. There's an **API** section with an "API Name" (starts with `AID...`) and an "API Token" - a separate credential pair from your website login, confirmed directly from WiGLE's own docs. Your actual account password never touches this app.

## Known gaps / next steps

- The Settings screen's Wi-Fi/BLE scan toggles are UI-only — `WarWalkingService` currently always scans everything. Wire the toggle state through to the service via intent extras if you want per-radio control.
- Event Board tab (Turf War time-limited leaderboards) is still a placeholder - this was part of the original design and hasn't been rebuilt against the WiGLE-direct architecture yet.
- Health Connect's step-verification path is implemented but not yet proven with real movement - everything tested so far has been from a stationary phone, so `fetchStepsDelta` has never actually returned a nonzero value in practice. Needs a real outdoor walk to confirm.
- No live view of currently-detected networks during a walk (like WiGLE's own app) - planned next: move Start Walk to the top of the dashboard and show the live scan list in the middle.
- A spatial "Local Legend" leaderboard (most APs mapped per neighborhood, not just globally) was scoped during the social-feed phase but never built, and doesn't fit the current architecture as-is (WiGLE's API doesn't expose per-neighborhood breakdowns) - would need rethinking, not just resuming.

## Optional: self-hosted backend (not required)

`backend/` is a fully working, independently-verified FastAPI + Postgres stack from an earlier design that routed registration, session sync, and a social feed (kudos/comments/leaderboard) through a self-hosted server instead of talking to WiGLE directly. It's kept in the repo because it's real, tested code that could be revived later if a social layer beyond WiGLE's own stats becomes worth building again - but the Android app no longer calls it for anything, and standing it up is not part of getting the app working.

If you want to explore it anyway:

```bash
cd backend
cp .env.example .env
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
# paste that key into .env as TOKEN_ENCRYPTION_KEY, and set a real POSTGRES_PASSWORD
docker compose up -d --build
```

Swagger UI: `http://<host>:8000/docs`.

## Verified

**WiGLE-direct pivot** (current architecture): the full rewrite - Room database, encrypted credential storage, direct WiGLE API calls, Profile/History/Settings screens - compiled clean on the first `assembleDebug` attempt. Installed and exercised live on the physical test device (Moto G Play 2024, Android 14): started a real walk, captured genuine neighboring Wi-Fi networks, stopped the session, and confirmed - all with zero backend running anywhere - that the session persisted to the local database, the streak correctly advanced to "1 Days Active" (computed live from that local data), the session appeared reactively in History, the rename dialog worked, and the Profile tab's not-signed-in state and local 7-day chart both rendered correctly with no crashes.

**Backend** (now optional, `backend/`): `./gradlew :app:assembleDebug` history aside, the FastAPI stack was independently validated earlier - all three migrations applied against a real local Postgres 16 with zero errors, and the app was run against it directly (not mocked): registration correctly round-tripped to the live WiGLE API and rejected bad credentials, and the full feed/kudos/comments/session flow (including ownership checks and idempotent kudos) was exercised end-to-end with `curl`.

Getting to a green build surfaced one thing no amount of code review would have caught: **AGP 9.0+ bundles Kotlin compilation directly into `com.android.application`** and now rejects the separate `org.jetbrains.kotlin.android` plugin outright, and the old `kotlinOptions { jvmTarget = ... }` DSL block goes with it (jvmTarget now just follows `compileOptions`). `android/build.gradle.kts` and `android/app/build.gradle.kts` are already written for the new model — if you're pattern-matching against older Android tutorials/AI output, expect them to still show the pre-9.0 two-plugin form.

## Compose BOM version note

Every other pinned version (AGP 9.3.0, Gradle 9.5.0, Kotlin 2.4.10, Health Connect 1.1.0, Room 2.8.4, security-crypto 1.1.0) was confirmed against live Maven/Google metadata while building this. The Compose BOM patch in `gradle/libs.versions.toml` (`2025.01.00`) could **not** be confirmed the same way — fetches for it kept returning inconsistent/stale-looking data. Run Android Studio's "Upgrade dependency" quick fix on first sync to confirm or bump it; don't treat that one number as verified.

## Fixes applied to the original draft

The pasted conversation had several bugs that would have failed silently or not compiled. Worth knowing about since they're easy to reintroduce if this code gets hand-edited from memory of the original chat:

- **Wrong WiGLE API root.** The draft hit `https://wigle.net` (the website) for both credential checks and file uploads. The real API is `https://api.wigle.net/api/v2/profile/user` (GET, Basic Auth) and `.../api/v2/file/upload` (POST multipart) — confirmed against WiGLE's published OpenAPI schema. `android/app/src/main/java/com/warwalking/app/network/WigleClient.kt`.
- **Invalid Kotlin (`async fun`, `Modifier.weight(1s)`).** `async fun` isn't Kotlin syntax (should be `suspend fun`); `1s` isn't a valid Float literal for `.weight()` (should be `1f`). Both would have failed to compile.
- **Split-brain session file ownership.** The draft's Activity and its foreground Service each created their own `SessionLogManager`, so the Activity could never find the file the Service actually wrote to disk, and the upload/cleanup logic was unreachable. Fixed by moving the whole session lifecycle (start → scan → stop → verify steps → upload → cleanup) into the Service itself, driven by `ACTION_START`/`ACTION_STOP` intents.
- **Wi-Fi scan results were counted but never geotagged or written to a file** in the original service sketch, despite the app's entire purpose being a WiGLE-format CSV upload. `WarWalkingService` now pairs every scan result with the last known GPS fix and writes a real `WigleWifi-1.4` CSV row.
- **BLE scanning was described in the architecture but never implemented** anywhere in the drafted code, despite requesting `BLUETOOTH_SCAN`/`CONNECT` permissions for it. Added.
- **"Live" step count would have required polling Health Connect continuously**, defeating the stated "no extra battery drain" design goal. Live UI feedback now comes from the free on-device `TYPE_STEP_COUNTER` sensor; Health Connect is only queried once, at session end, as the authoritative anti-cheat source of truth.
- **`registerReceiver` without an exported flag** throws on API 33+ when a context-registered receiver is involved. Now conditionally passes `RECEIVER_NOT_EXPORTED`.
- **10-minute wake lock timeout** on a session with no renewal logic — the CPU could sleep mid-walk while the scan loop kept "running." Bumped to a 6-hour safety-net timeout release on session stop instead.
- **CSV injection risk**: an SSID containing a comma or quote would have corrupted the WigleWifi CSV row structure. `SessionLogManager` now quotes/escapes fields that need it.
- **Streak logic that never decayed.** The original design (a Postgres trigger, since replaced) only updated the streak counter on a new session insert, so it stayed frozen at whatever it was if you stopped walking - never resetting to 0 on its own. `StreakCalculator.kt` recomputes from full history on every read instead, so a lapsed streak correctly shows 0.
