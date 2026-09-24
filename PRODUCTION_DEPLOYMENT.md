# 🚀 LivePulse Production Deployment Guide

This guide provides end-to-end, step-by-step instructions and scripts for deploying **LivePulse** to production, configuring **Firebase Cloud Firestore**, setting up **Google Identity Services (OAuth 2.0)**, signing the release build, and publishing to the **Google Play Store**.

---

## 📑 Table of Contents
1. [Prerequisites & System Requirements](#1-prerequisites--system-requirements)
2. [Step 1: Firebase Project & Authentication Setup](#2-step-1-firebase-project--authentication-setup)
3. [Step 2: SHA-1 Fingerprint & Google Sign-In Setup](#3-step-2-sha-1-fingerprint--google-sign-in-setup)
4. [Step 3: Deploying Firestore Security Rules](#4-step-3-deploying-firestore-security-rules)
5. [Step 4: Release Keystore & App Signing](#5-step-4-release-keystore--app-signing)
6. [Step 5: Compiling Production App Bundle (.AAB) & APK](#6-step-5-compiling-production-app-bundle-aab--apk)
7. [Step 6: Google Play Console Release Checklist](#7-step-6-google-play-console-release-checklist)
8. [Step 7: Browser Access: Docs and Demo Site Hosting](#8-step-7-browser-access-docs-and-demo-site-hosting)
9. [Step 8: Production Troubleshooting & Cost Safeguards](#9-step-8-production-troubleshooting--cost-safeguards)

---

## 1. Prerequisites & System Requirements

Ensure you have the following installed on your development/CI machine:
- **JDK 17 or JDK 21**: Required for Android Gradle Plugin 8.9+.
- **Android SDK (API 35)**: Build-Tools 35.0.0 and Platform Tools.
- **Node.js & Firebase CLI** *(Optional, for rules deployment)*: `npm install -g firebase-tools`
- **Application ID**: `com.aistudio.livepulse.xqmtw` (configured in `app/build.gradle.kts`).

---

## 2. Step 1: Firebase Project & Authentication Setup

LivePulse uses Firebase for **Cloud Firestore (Multiplayer live sync)** and **Firebase Auth (Google Sign-In)** on the **Spark Free Tier ($0/month)**.

### Manual Steps in Firebase Console:
1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **Add Project**.
2. Name your project (e.g., `livepulse-prod`) and select or disable Google Analytics as preferred.
3. In the project dashboard, click the **Android icon** (➕ Add app) to register an Android app:
   - **Android package name**: `com.aistudio.livepulse.xqmtw` *(must match `applicationId` in `app/build.gradle.kts`)*.
   - **App nickname**: `LivePulse Production`.
   - **Debug signing certificate SHA-1**: *(See Step 2 below)*.
4. Download the generated **`google-services.json`** file.
5. Move `google-services.json` into the root **`app/`** directory:
   ```bash
   cp ~/Downloads/google-services.json app/google-services.json
   ```

### Enable Authentication & Firestore:
1. **Enable Google Sign-In**:
   - Go to **Build ➔ Authentication ➔ Sign-in method**.
   - Click **Google** and toggle **Enable**.
   - Select your project support email and click **Save**.
2. **Enable Facebook Sign-In**:
   - Register your app on [Meta for Developers](https://developers.facebook.com/).
   - Obtain your **App ID** and **App Secret**.
   - In Firebase Console ➔ **Authentication ➔ Sign-in method**, click **Facebook**, toggle **Enable**, and enter your Facebook App ID & Secret.
3. **Enable Apple Sign-In**:
   - Register your Services ID and Apple Developer Team on [Apple Developer Portal](https://developer.apple.com/).
   - In Firebase Console ➔ **Authentication ➔ Sign-in method**, click **Apple**, toggle **Enable**, and enter your Services ID, Apple Team ID, Key ID, and Private Key.
4. **Enable Cloud Firestore**:
   - Go to **Build ➔ Firestore Database ➔ Create database**.
   - Select a database region close to your users (e.g., `asia-southeast1`, `us-central1`, or `europe-west1`).
   - Choose **Start in production mode** (we will apply rules in Step 3).

---

## 3. Step 2: SHA-1 Fingerprint & Google Sign-In Setup

Google Sign-In requires your app's **SHA-1 and SHA-256 certificate fingerprints** registered in Firebase and Google Cloud Console to prevent unauthorized API access.

### 1. Extract Local Debug SHA-1 (for local testing):
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep -E "SHA1|SHA256"
```

### 2. Generate Production Release Keystore:
Run the provided helper script:
```bash
bash scripts/generate-release-keystore.sh
```
Or manually generate it:
```bash
keytool -genkeypair -v -keystore release.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

### 3. Register Fingerprints in Firebase:
1. In Firebase Console, go to **Project Settings ➔ General ➔ Your apps ➔ LivePulse Android**.
2. Click **Add fingerprint** and paste your:
   - **Debug SHA-1**
   - **Release SHA-1**
   - **Release SHA-256**
3. Re-download `google-services.json` and replace `app/google-services.json`.

### 4. Configure the Web Client ID for Google Sign-In:
1. In the [Google Cloud Console](https://console.cloud.google.com/apis/credentials) ➔ **APIs & Services ➔ Credentials**, open **Web client (auto created by Google Service)** and copy its **Client ID**.
2. Copy `.env.example` to `.env` at the **repo root** (git-ignored; the secrets Gradle plugin reads the root-level file) and set:
   ```properties
   WEB_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
   ```
3. The app reads this value via `BuildConfig.WEB_CLIENT_ID` in `GoogleAuthHelper.kt`. Without it, builds ship a placeholder ID — `GoogleAuthHelper` logs a warning at sign-in time and Google Sign-In fails. A valid ID looks like `1234567890-abcdef.apps.googleusercontent.com` (numeric prefix required).

---

## 4. Step 3: Deploying Firestore Security Rules

LivePulse includes a pre-configured `firestore.rules` file protecting poll ownership, voting rights, and subcollections.

### Option A: Deploy via Firebase CLI (Recommended)
```bash
# 1. Log in to Firebase
firebase login

# 2. Select your Firebase project
firebase use --add

# 3. Deploy the rules
bash scripts/deploy-firestore-rules.sh
```

### Option B: Manual Setup via Firebase Console
1. Go to **Firebase Console ➔ Firestore Database ➔ Rules tab**.
2. Paste the contents of `/firestore.rules`:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAuthenticated() {
      return request.auth != null;
    }
    function isDocCreator(pollData) {
      return isAuthenticated() && (request.auth.uid == pollData.creatorId || pollData.creatorId == null);
    }

    match /polls/{pollId} {
      allow read: if true;
      allow create: if request.resource.data.code is string 
                    && request.resource.data.title is string;
      allow update: if isDocCreator(resource.data) || !resource.data.isClosed;
      allow delete: if isDocCreator(resource.data);

      match /votes/{voteId} {
        allow read: if true;
        allow create, update: if request.resource.data.pollId == pollId
                              && request.resource.data.optionId is string;
        allow delete: if isAuthenticated() && (request.auth.uid == resource.data.voterId);
      }
    }
  }
}
```
3. Click **Publish**.

---

## 5. Step 4: Release Keystore & App Signing

The release signing config in `app/build.gradle.kts` reads its values from **environment variables** (`System.getenv`) — putting them in `gradle.properties` has no effect. Export them in your shell (or CI secret store) before building:

```bash
# key alias is fixed to "upload" in app/build.gradle.kts
export KEYSTORE_PATH=/absolute/path/to/release.keystore
export STORE_PASSWORD='<your-keystore-password>'
export KEY_PASSWORD='<your-key-password>'
```

Keep the keystore file and these values out of the repository.

### Signing in CI (GitHub Actions)

The `release_bundle` job in `.github/workflows/android_ci_cd.yml` produces a **signed** `.aab` on every push to `main` or `release/*` — but it stays dormant until you opt in. (The job is gated because `app/build.gradle.kts` applies the release signing config unconditionally; without a real keystore, `bundleRelease` would fail with a cryptic signing error.)

If you don't have a release keystore yet, generate one with `bash scripts/generate-release-keystore.sh` (alias `upload`; the script's default password is `livepulse2026` — change it if you like, but the CI secrets below must match).

Configure once in **GitHub ➔ Settings ➔ Secrets and variables ➔ Actions**:

| Item | Tab | Value |
| :--- | :--- | :--- |
| `RELEASE_ENABLED` | Variables | `true` — the non-secret on/off switch |
| `RELEASE_KEYSTORE_BASE64` | Secrets | your entire `release.keystore`, base64-encoded |
| `STORE_PASSWORD` | Secrets | the keystore's store password |
| `KEY_PASSWORD` | Secrets | the key's password |
| `WEB_CLIENT_ID` *(optional but recommended)* | Secrets | the OAuth Web Client ID — the workflow writes it into a repo-root `.env` so Google Sign-In works in the release bundle |

To encode the keystore on macOS:
```bash
base64 -i release.keystore -o release.keystore.b64
```
Paste the output into `RELEASE_KEYSTORE_BASE64`. Keep the original file safe and offline — if you lose it, you can never publish an update to the same Play Store listing.

On the next qualifying push (after `build_and_test` passes), the workflow:
1. Fails fast if `RELEASE_ENABLED` is `true` but `RELEASE_KEYSTORE_BASE64` is missing (deliberate guard).
2. Decodes the keystore onto the runner.
3. Writes a repo-root `.env` from the `WEB_CLIENT_ID` secret when set (otherwise emits a warning — the bundle ships without Google Sign-In configured).
4. Runs `./gradlew :app:bundleRelease` with `KEYSTORE_PATH` pointing at the decoded file — producing a signed `app-release.aab`.
5. Uploads it as the `app-release-aab` artifact (30-day retention) under the run's **Artifacts** section.

Uploading to the Play Store itself stays manual (see Step 6). To disable the job again, flip `RELEASE_ENABLED` to `false` or delete the variable.

---

## 6. Step 5: Compiling Production App Bundle (.AAB) & APK

Google Play Store requires the **Android App Bundle (.aab)** format for production distribution.

Run the production build script:
```bash
bash scripts/build-release.sh
```

### Output Build Artifacts:
- 📦 **Google Play Bundle (AAB)**:  
  `app/build/outputs/bundle/release/app-release.aab`
- 📱 **Standalone Release APK (for Direct Sideloading / QA)**:  
  `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 7. Step 6: Google Play Console Release Checklist

1. **Create Application in Google Play Console**:
   - Application Name: **LivePulse**
   - Default Language: **English (United States)**
   - App or Game: **App**
   - Free or Paid: **Free**

2. **Store Listing Assets**:
   - **App Icon**: 512 × 512 px 32-bit PNG.
   - **Feature Graphic**: 1024 × 500 px JPG or PNG.
   - **Phone Screenshots**: Minimum 2 screenshots (1080 × 1920 px or 1080 × 2400 px).
   - **Short Description** (up to 80 chars):  
     *Real-time group polling, voting consensus, and RSVP coordination.*
   - **Full Description**: Copy features from `docs/index.html` or `README.md`.

3. **Data Safety Declaration**:
   - **Personal Info**: Name and Email *(Collected only when using Google Sign-In for voter identification, Ephemeral/Optional)*.
   - **Data Transmission**: Data encrypted in transit via TLS/HTTPS.
   - **User Data Deletion**: Supported (users can clear data or delete created polls).

4. **Upload App Bundle (.aab)**:
   - Go to **Release ➔ Production (or Internal Testing)** ➔ **Create new release**.
   - Upload `app-release.aab`.
   - Add Release Notes.
   - Review and rollout to testers!

---

## 8. Step 7: Browser Access: Docs and Demo Site Hosting

Users without an Android device can still explore LivePulse in the browser. The `docs/` directory is a self-contained static site — this documentation plus an interactive, client-side demo of the voting flow. It makes **no Firebase calls**; real-time voting, RSVP, and offline sync run in the Android app.

### 1. Deploy `docs/` to Firebase Hosting:
```bash
firebase init hosting
# Select your Firebase project
# Public directory: docs
# Configure as single-page app: No
firebase deploy --only hosting
```

### 2. Share the Hosted URL with Voters:
1. Voters open the URL in any browser (desktop, laptop, Chromebook, iOS) — nothing to install or sideload.
2. The interactive demo simulates joining a poll with a 6-character poll code (e.g. `SOC5V5`), voting, and RSVP flows.
3. To cast real votes, install the Android app and sign in — votes sync live via Firestore snapshot listeners.

---

## 9. Step 8: Production Troubleshooting & Cost Safeguards

| Symptom | Probable Cause | Fix / Resolution |
| :--- | :--- | :--- |
| **Google Sign-In Error code `10` or `12500`** | Missing/incorrect SHA-1 fingerprint in Firebase Console, or `WEB_CLIENT_ID` not set/invalid in `.env` (repo root). | Run `bash scripts/generate-release-keystore.sh`, copy the SHA-1, add it to Firebase Console ➔ Project Settings, and replace `google-services.json`. Then set the Web Client ID in `.env` (see Step 2 §4). |
| **Firestore `PERMISSION_DENIED`** | Security rules rejected the write operation. | Deploy the official `firestore.rules` using `bash scripts/deploy-firestore-rules.sh`. |
| **App crashes on startup with `FirebaseApp not initialized`** | `google-services.json` is missing from the `/app` root directory. | Download `google-services.json` from Firebase Console and place it in the `app/` folder. |
| **Cost Alert Protection** | Exceeding 50,000 reads/day. | LivePulse is engineered with a **Room-first SQLite Cache**; local reads hit device SQLite and cost $0.00. Set a budget alert in Google Cloud Billing at $1.00 for safety. |

---

**LivePulse is now ready for production release! 🚀**
