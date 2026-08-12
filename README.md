# WarWalker

A gamified wardriving fitness app: scan Wi-Fi/BLE with your phone, verify the walk with Health Connect step data, upload the session to WiGLE, and compete on a self-hosted leaderboard. Android + Android Wear only.

This repo was scaffolded from a design conversation (originally drafted with Gemini) covering the architecture, DB schema, FastAPI backend, and Kotlin client. Claude reviewed that draft for correctness before turning it into a real, buildable project — see **Fixes applied** below for what was wrong in the original and why.

## Layout

```
backend/            FastAPI + PostgreSQL, deploys via Docker Compose
  app/               API source
  migrations/        SQL, auto-applied on first DB boot
android/            Native Kotlin app (Jetpack Compose, Health Connect, Retrofit)
```

## Running the backend

```bash
cd backend
cp .env.example .env
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
# paste that key into .env as TOKEN_ENCRYPTION_KEY, and set a real POSTGRES_PASSWORD
docker compose up -d --build
```

Swagger UI: `http://<host>:8000/docs`. The `migrations/*.sql` files run automatically the first time the `warwalking-db` volume is created (via Postgres's `docker-entrypoint-initdb.d`); they will **not** re-run against an existing volume, so wipe `pgdata` (or apply new migrations manually) if you change the schema later.

## Opening the Android app

1. Open `android/` in Android Studio (SDK 37 / build-tools 36.0.0 already detected on this machine; AGP 9.3.0 needs Gradle 9.5.0 + JDK 17, both satisfied here).
2. In `app/build.gradle.kts`, point `BACKEND_BASE_URL` at your homelab server:
   - Android **emulator**: `http://10.0.2.2:8000/` (already the default — `10.0.2.2` is the emulator's alias for your host machine).
   - **Physical phone/watch**: your Proxmox host's real LAN IP, e.g. `http://192.168.1.50:8000/`.
3. Debug builds allow cleartext HTTP to reach an unencrypted homelab server (`app/src/debug/res/xml/network_security_config.xml`); release builds require HTTPS. Put the backend behind TLS (Caddy/nginx + a cert) before shipping a release build.
4. First sync will need network access to resolve Gradle/AGP/Kotlin plugin versions and the exact Compose BOM patch — Android Studio's dependency-upgrade inspection will flag anything stale (see note below).

## Known gaps / next steps

- The Settings screen's Wi-Fi/BLE scan toggles are UI-only — `WarWalkingService` currently always scans everything. Wire the toggle state through to the service via intent extras if you want per-radio control.
- Event Board and History tabs are placeholders. Backend endpoints exist (`GET /api/events/active`, `GET /api/events/{id}/leaderboard`); there's no `GET /api/sessions` list endpoint yet for History.
- The streak count shown on the dashboard isn't fetched from the backend yet (`user_streaks` table + trigger exist and work — no endpoint exposes it yet).
- `TOKEN_ENCRYPTION_KEY` in `.env` is the only thing standing between a DB leak and every researcher's WiGLE token — treat that key like a production secret, not a repo file.

## Verified

`./gradlew :app:assembleDebug` succeeds end-to-end on this machine (Gradle 9.5.0, AGP 9.3.0, JDK 17 via Android Studio's bundled JBR) and produces a real `app-debug.apk`. `python3 -m py_compile` passes on all backend modules and the `docker-compose.yml` parses as valid YAML. Actually installing/running the app on a device, and standing up the Docker stack against a live Postgres, haven't been done yet.

Getting to a green build surfaced one thing no amount of code review would have caught: **AGP 9.0+ bundles Kotlin compilation directly into `com.android.application`** and now rejects the separate `org.jetbrains.kotlin.android` plugin outright, and the old `kotlinOptions { jvmTarget = ... }` DSL block goes with it (jvmTarget now just follows `compileOptions`). `android/build.gradle.kts` and `android/app/build.gradle.kts` are already written for the new model — if you're pattern-matching against older Android tutorials/AI output, expect them to still show the pre-9.0 two-plugin form.

## Compose BOM version note

Every other pinned version (AGP 9.3.0, Gradle 9.5.0, Kotlin 2.4.10, Health Connect 1.1.0) was confirmed against live Maven/Google metadata while scaffolding this. The Compose BOM patch in `gradle/libs.versions.toml` (`2025.01.00`) could **not** be confirmed the same way — fetches for it kept returning inconsistent/stale-looking data. Run Android Studio's "Upgrade dependency" quick fix on first sync to confirm or bump it; don't treat that one number as verified.

## Fixes applied to the original draft

The pasted conversation had several bugs that would have failed silently or not compiled. Worth knowing about since they're easy to reintroduce if this code gets hand-edited from memory of the original chat:

- **Wrong WiGLE API root.** The draft hit `https://wigle.net` (the website) for both credential checks and file uploads. The real API is `https://api.wigle.net/api/v2/profile/user` (GET, Basic Auth) and `.../api/v2/file/upload` (POST multipart) — confirmed against WiGLE's published OpenAPI schema. `backend/app/wigle_client.py`.
- **Plaintext WiGLE tokens at rest.** The schema comment said "Encrypted or stored token" but nothing ever encrypted it. Tokens are now Fernet-encrypted before insert and decrypted only in-memory right before use. `backend/app/crypto.py`.
- **Invalid Kotlin (`async fun`, `Modifier.weight(1s)`).** `async fun` isn't Kotlin syntax (should be `suspend fun`); `1s` isn't a valid Float literal for `.weight()` (should be `1f`). Both would have failed to compile.
- **`asyncpg.UniqueViolationError` referenced without importing `asyncpg`** in the FastAPI draft — `NameError` at the first duplicate registration.
- **Retrofit repository called nonexistent named parameters** (`steps_counted =`, `ap_discovered =`) against a function whose actual Kotlin parameters were camelCase — compile error.
- **Split-brain session file ownership.** The draft's Activity and its foreground Service each created their own `SessionLogManager`, so the Activity could never find the file the Service actually wrote to disk, and the upload/cleanup logic was unreachable. Fixed by moving the whole session lifecycle (start → scan → stop → verify steps → upload → cleanup) into the Service itself, driven by `ACTION_START`/`ACTION_STOP` intents.
- **Wi-Fi scan results were counted but never geotagged or written to a file** in the original service sketch, despite the app's entire purpose being a WiGLE-format CSV upload. `WarWalkingService` now pairs every scan result with the last known GPS fix and writes a real `WigleWifi-1.4` CSV row.
- **BLE scanning was described in the architecture but never implemented** anywhere in the drafted code, despite requesting `BLUETOOTH_SCAN`/`CONNECT` permissions for it. Added.
- **"Live" step count would have required polling Health Connect continuously**, defeating the stated "no extra battery drain" design goal. Live UI feedback now comes from the free on-device `TYPE_STEP_COUNTER` sensor; Health Connect is only queried once, at session end, as the authoritative anti-cheat source of truth.
- **`registerReceiver` without an exported flag** throws on API 33+ when a context-registered receiver is involved. Now conditionally passes `RECEIVER_NOT_EXPORTED`.
- **10-minute wake lock timeout** on a session with no renewal logic — the CPU could sleep mid-walk while the scan loop kept "running." Bumped to a 6-hour safety-net timeout release on session stop instead.
- **Global cleartext HTTP** via a blanket `usesCleartextTraffic` was avoided in favor of a network security config that only permits it in debug builds.
- **Hardcoded Postgres password committed in `docker-compose.yml`**. Moved to `.env` (gitignored), with `.env.example` checked in instead.
- **CSV injection risk**: an SSID containing a comma or quote would have corrupted the WigleWifi CSV row structure. `SessionLogManager` now quotes/escapes fields that need it.
- **Streak trigger used `CURRENT_DATE`** (the server's "now") to decide which calendar day a walk counted toward, which misattributes a session synced late (e.g. phone was offline overnight) to the wrong day. Changed to derive the day from the session's own `end_time`.
