# LivePulse — Launch Plan & Handoff Ledger

Updated: 2026-11-02. Working state across machines. No secrets in this file —
secret locations are named, values are not.

## Where things stand

| Area | State |
|---|---|
| App identity | `com.plutusoft.livepulse` (permanent), versionCode 1, "1.0" |
| Firebase | Project `livepulse-prod`; new Android app registered with debug SHA-1 + upload-key SHA-1/SHA-256. Google Sign-In verified on-device (debug build, Oct). **Play App Signing SHA not yet in Firebase** — pending first Play-track install (see Next actions). |
| Release signing | `release.keystore` (repo root, alias `upload`, valid to 2054). Password in password manager + gitignored `keystore.properties`. Signed AAB verified locally and by CI. |
| CI | Green. Push to `main` → tests + lint + signed `app-release-aab` artifact (GitHub secrets: RELEASE_KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD, WEB_CLIENT_ID, GOOGLE_SERVICES_JSON; variable RELEASE_ENABLED=true). Verified run: 36302643646. |
| Play Console | Account `plutusoft` (personal, ID 5408416651142886853). App LivePulse: internal release `1.0-internal-1` **live** since Oct 10, "Available to internal testers", 0 installs (no testers until Nov 1). Tester email list created Nov 1 ✓ (after Chrome update + committing email as chip). |
| Store listing | Title/short/full description, icon-512, feature graphic, 5 screenshots — all in `docs/play-assets/` + `docs/play-store-listing.md`; core listing entered in console Oct 10. |
| Privacy policy | Live: https://trantankhoa.github.io/livepoll/privacy.html (source `docs/privacy.html`) |
| Old app entry | `com.aistudio.livepulse.xqmtw` in Firebase — DELETE once Play-build sign-in is verified (safe: only breaks the old debug install) |

## Next actions (the live thread)

1. **Opt-in link said "item not found" on phone (Nov 1).** Two fixes, both cheap:
   - Add ALL Google accounts used on the phone to the tester list (console: Testers tab → edit list) — `khoa.trantan2000@gmail.com` is in; add the others (`khoa.tran.family@`, `khoa.tran@indebted.co`) if the phone's Play Store uses one of them.
   - If account is right: propagation delay — first tester-list attach can take minutes–hours. Retry later; the link wakes up on its own.
2. **Phone installed from Play** → plug phone in over USB → from repo root:
   - `adb shell pm path com.plutusoft.livepulse` → pull each APK → `keytool -printcert -jarfile base.apk`
   - That yields the **Play App Signing SHA-1/SHA-256** (Google's key). Add to Firebase → Project settings → Android app `com.plutusoft.livepulse` → Add fingerprint. Then Play-build Google Sign-In works. (Fingerprint must come from the device or the console's App integrity page — never transcribe from a screenshot.)
3. **Release smoke on device** (needs no Play): `./gradlew assembleRelease --no-build-cache` → install `app/build/outputs/apk/release/app-release.apk` (upload-key signed; Firebase already trusts that key) → verify sign-in + create poll + vote.
4. **Start the production clock**: invite ~15–20 testers (Google requires roughly 12–20 opted-in testers for 14 continuous days before a new personal account may publish to production). Invite draft exists in this file's history / rewrite: link + "Become a tester" + install + vote. Testers must INSTALL, not just opt in. Track the 14 days from the console dashboard's testing tracker.
5. **During the 14 days — production forms** (answers ready in `docs/play-store-listing.md`):
   - App content: ads (none), content rating questionnaire, target audience (18+ only), data safety (Name/Email/User ID/user content; app functionality; encrypted in transit; deletion yes; no sharing/sale), app access (all available), countries (all free).
   - Optional engineering before production: enable R8 (`isMinifyEnabled`) + upload mapping.txt as CI artifact; run the full compliance audit (`.claude/skills/play-store/` skill pack — run INLINE, no subagents per global policy).
6. **Day 15+**: production-access questionnaire (console) → Production → Create release (reuse AAB or bump versionCode 2) → staged rollout 10–20% → review (hours–7 days) → live.

## Machine move — what must travel manually

Everything gitignored is NOT in the clone. Copy these from the old machine
(`~/plutusoft/livepoll/`) to the same paths on the new one:

- `release.keystore` — **the** signing key. Losing it is survivable (Play App Signing enrolled → Google support can reset the upload key in days) but painful: keep a copy in the password manager as a file attachment, not just on disk.
- `keystore.properties` — store/key passwords (also in password manager).
- `.env` — `WEB_CLIENT_ID=…` (root-level, read by secrets-gradle-plugin).
- `app/google-services.json` — Firebase config (current copy has both app entries; re-downloadable from Firebase console if lost).
- `debug.keystore` — only matters for debug sign-in continuity; debug SHA-1 is registered in Firebase.

CI-built artifacts re-download anytime: `gh run download 36302643646 --name app-release-aab`
(artifact retention 30 days; after that, any push to main produces a fresh one).

## New machine setup (macOS)

```bash
brew install openjdk@17 android-commandlinetools
# JAVA — macOS /usr/bin/keytool is a stub; export the real JDK:
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
# adb lives at /opt/homebrew/share/android-commandlinetools/platform-tools/adb
# Build (always --no-build-cache; this repo's cache has served stale artifacts twice):
./gradlew assembleDebug --no-build-cache
```

Phone USB debugging is authorized per-machine: first `adb devices` on the new
machine prompts an RSA dialog **on the phone** — accept it.

## Hard-won gotchas (do not relearn these)

- Always `--no-build-cache` (stale-artifact corruption, twice).
- Root `.env` only — secrets plugin doesn't read `app/.env`. Never put secrets in `.env` that shouldn't become `BuildConfig` (they ship inside the APK).
- `missingGoogleServicesStrategy = WARN` means a missing `app/google-services.json` still builds — CI restores it from the `GOOGLE_SERVICES_JSON` secret (added for exactly this; a CI AAB without it would have shipped dead).
- Play Console greying "Save changes": outdated Chrome + email not committed as chip. Update browser; click the dropdown entry so the address becomes a pill.
- Play Console navigation: the app row is below the fold on the home page; nothing works until you click INTO the app.
- Roborazzi UX harness tests (`UxCatalogScreenshotTest`) are demo-gated (`Assume.assumeTrue(ENABLE_DEMO_DATA)`) — run with `-PdemoData=true` to regenerate listing screenshots; default runs skip them (CI-safe).
- git-pushing CLI refuses `.env.example` / `*keystore*` paths by name — `--allow-risky` after verifying contents.

## Key references

- Firebase console: https://console.firebase.google.com → `livepulse-prod`
- Play Console: https://play.google.com/console (account plutusoft, personal)
- CI run with verified signed AAB: https://github.com/trantankhoa/livepoll/actions/runs/36302643646
- Firestore rules deploy: `bash scripts/deploy-firestore-rules.sh` (already live)
- This ledger: keep `.notes/plan.md` updated as items complete; it is the cross-machine, cross-session source of truth.
