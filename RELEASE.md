# Release prep notes (WarWalker v1.0.0)

Everything below was prepared in one pass on 2026-08-12. Nothing has been
committed, pushed, or submitted anywhere yet - this file is the map of
what's done vs. what still needs your accounts/identity.

## What's done

- **Event Board tab hidden.** It was a non-functional placeholder; removed
  from bottom nav for v1 (`MainActivity.kt`). Re-add when it's actually
  built.
- **Release signing.** New keystore at `/Users/n0d3/keystores/war-walker-release.jks`
  (RSA 4096, valid to 2056). Wired into `android/app/build.gradle.kts` via
  `android/keystore.properties` (gitignored, not committed). **Back up both
  files somewhere durable and off this one machine** - a password manager
  or encrypted cloud storage, not just this disk. If you lose the keystore,
  you can never publish an update to the same Play Store listing again;
  Google cannot recover or reset it for you.
- **Version bumped** 0.1.0 → 1.0.0 (versionCode stays 1 - first release).
- **Minification + resource shrinking enabled** for the release build type,
  with ProGuard keep rules added for the Gson-parsed WiGLE API models and
  Room entities (`android/app/proguard-rules.pro`). Verified: a signed,
  minified `./gradlew :app:assembleRelease` builds clean and
  `apksigner verify` confirms the release cert.
- **Privacy policy** drafted and build-verified at
  `/Users/n0d3/Projects/dugcanlift-site/war-walker-privacy.md` → will be
  live at `https://www.dugcanlift.com/war-walker/privacy/` once that repo
  is pushed (not pushed yet - see checklist).
- **Store listing copy + icon + feature graphic** at
  `fastlane/metadata/android/en-US/` in this repo (title, short/full
  description, v1 changelog, a composited 512x512 icon, and a 1024x500
  feature graphic). This layout is read directly by F-Droid's build
  process and can also be fed to `fastlane supply` for Play if you
  automate uploads later.
- **F-Droid build recipe drafted** at `docs/fdroid/com.warwalking.app.yml`
  - not submitted. See the comments in that file for the submission steps
  and an important caveat: WiGLE-dependency likely earns a `NonFreeNet`
  antifeature tag, flagged there for you to confirm against current
  F-Droid policy rather than taking my read as final.

## Play Store Data Safety form - my best-effort mapping

Google holds you accountable for this questionnaire, and its exact
category list changes over time - verify against the live Play Console
form rather than trusting this blindly. Based on what the app actually
does today:

| Data type | Collected? | Shared? | Purpose | Notes |
|---|---|---|---|---|
| Location (precise) | Yes | Yes - with WiGLE | App functionality | Sent per-network, only during an active walk you started |
| Health & fitness (steps/distance/calories) | Yes | No | App functionality | Read once per walk from Health Connect, used for local scoring only, never uploaded |
| App activity / other (Wi-Fi & BLE network info) | Yes | Yes - with WiGLE | App functionality | No exact Play category fits "nearby network SSIDs/BSSIDs" - closest is App activity; note it explicitly in the listing description regardless |
| Personal info / account credentials | No | No | - | Your WiGLE API Name/Token authenticates *your* requests to WiGLE's API; it's not data WarWalker collects about you for its own purposes, and never reaches any WarWalker-controlled server (there is none) |

Also declare: no data sold, users can request deletion (uninstall removes
everything local; WiGLE-side data deletion goes through WiGLE, not you -
the privacy policy says this explicitly, link it from this form).

## What only you can do from here

1. **Push the privacy policy live**: commit + push
   `war-walker-privacy.md` in the `dugcanlift-site` repo, confirm
   `https://www.dugcanlift.com/war-walker/privacy/` resolves. Needed
   before Play Console will accept your submission.
2. **Commit the app changes** in this repo (signing config wiring, version
   bump, ProGuard rules, Event Board removal, fastlane metadata, F-Droid
   recipe draft) - I've left these uncommitted so you can review the diff
   first.
3. **Take real screenshots.** Both stores require actual device
   screenshots (phone, 2-8 images). I can't generate these without running
   the app on a device/emulator - run it yourself in Android Studio or on
   your Moto G Play test device and grab a few from the Walk, Profile, and
   History tabs.
4. **Google Play Console account**: $25 one-time registration fee, your
   own Google account - I can't create or pay for this. After that,
   Google's closed-testing requirement for new personal developer accounts
   currently means 20+ testers opted in for 14+ continuous days before
   you can request production access - budget time for this before your
   app is publicly visible.
5. **Submit the F-Droid merge request** using `docs/fdroid/com.warwalking.app.yml`
   as a starting point once you've tagged `v1.0.0` - see that file's header
   comment for the exact steps. This is a public submission under your
   GitHub/GitLab identity, so it's yours to send.
6. **Double-check the Data Safety table above** against the live Play
   Console form before submitting - form field names/categories do change.
