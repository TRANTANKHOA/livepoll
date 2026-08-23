# 🚀 PulsePoll Production Deployment Guide

This guide provides end-to-end, step-by-step instructions and scripts for deploying **PulsePoll** to production, configuring **Firebase Cloud Firestore**, setting up **Google Identity Services (OAuth 2.0)**, signing the release build, and publishing to the **Google Play Store**.

---

## 📑 Table of Contents
1. [Prerequisites & System Requirements](#1-prerequisites--system-requirements)
2. [Step 1: Firebase Project & Authentication Setup](#2-step-1-firebase-project--authentication-setup)
3. [Step 2: SHA-1 Fingerprint & Google Sign-In Setup](#3-step-2-sha-1-fingerprint--google-sign-in-setup)
4. [Step 3: Deploying Firestore Security Rules](#4-step-3-deploying-firestore-security-rules)
5. [Step 4: Release Keystore & App Signing](#5-step-4-release-keystore--app-signing)
6. [Step 5: Compiling Production App Bundle (.AAB) & APK](#6-step-5-compiling-production-app-bundle-aab--apk)
7. [Step 6: Google Play Console Release Checklist](#7-step-6-google-play-console-release-checklist)
8. [Step 7: Production Troubleshooting & Cost Safeguards](#8-step-7-production-troubleshooting--cost-safeguards)

---

## 1. Prerequisites & System Requirements

Ensure you have the following installed on your development/CI machine:
- **JDK 17 or JDK 21**: Required for Android Gradle Plugin 8.9+.
- **Android SDK (API 35)**: Build-Tools 35.0.0 and Platform Tools.
- **Node.js & Firebase CLI** *(Optional, for rules deployment)*: `npm install -g firebase-tools`
- **Application ID**: `com.aistudio.pollpulse.qxrvlz` (configured in `app/build.gradle.kts`).

---

## 2. Step 1: Firebase Project & Authentication Setup

PulsePoll uses Firebase for **Cloud Firestore (Multiplayer live sync)** and **Firebase Auth (Google Sign-In)** on the **Spark Free Tier ($0/month)**.

### Manual Steps in Firebase Console:
1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **Add Project**.
2. Name your project (e.g., `pulsepoll-production`) and select or disable Google Analytics as preferred.
3. In the project dashboard, click the **Android icon** (➕ Add app) to register an Android app:
   - **Android package name**: `com.aistudio.pollpulse.qxrvlz` *(must match `applicationId` in `app/build.gradle.kts`)*.
   - **App nickname**: `PulsePoll Production`.
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
3. **Enable Cloud Firestore**:
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
keytool -genkeypair -v -keystore release.keystore -alias pulsepoll -keyalg RSA -keysize 2048 -validity 10000
```

### 3. Register Fingerprints in Firebase:
1. In Firebase Console, go to **Project Settings ➔ General ➔ Your apps ➔ PulsePoll Android**.
2. Click **Add fingerprint** and paste your:
   - **Debug SHA-1**
   - **Release SHA-1**
   - **Release SHA-256**
3. Re-download `google-services.json` and replace `app/google-services.json`.

---

## 4. Step 3: Deploying Firestore Security Rules

PulsePoll includes a pre-configured `firestore.rules` file protecting poll ownership, voting rights, and subcollections.

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

To configure Gradle to sign your release APK/AAB automatically, you can provide signing properties via environment variables or `gradle.properties`:

### Add to `~/.gradle/gradle.properties` (or CI Secret Store):
```properties
PULSEPOLL_RELEASE_STORE_FILE=../release.keystore
PULSEPOLL_RELEASE_KEY_ALIAS=pulsepoll
PULSEPOLL_RELEASE_STORE_PASSWORD=pulsepoll2026
PULSEPOLL_RELEASE_KEY_PASSWORD=pulsepoll2026
```

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
   - Application Name: **PulsePoll**
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

## 8. Step 7: Production Troubleshooting & Cost Safeguards

| Symptom | Probable Cause | Fix / Resolution |
| :--- | :--- | :--- |
| **Google Sign-In Error code `10` or `12500`** | Missing or incorrect SHA-1 fingerprint in Firebase Console. | Run `bash scripts/generate-release-keystore.sh`, copy the SHA-1, add it to Firebase Console ➔ Project Settings, and replace `google-services.json`. |
| **Firestore `PERMISSION_DENIED`** | Security rules rejected the write operation. | Deploy the official `firestore.rules` using `bash scripts/deploy-firestore-rules.sh`. |
| **App crashes on startup with `FirebaseApp not initialized`** | `google-services.json` is missing from the `/app` root directory. | Download `google-services.json` from Firebase Console and place it in the `app/` folder. |
| **Cost Alert Protection** | Exceeding 50,000 reads/day. | PulsePoll is engineered with a **Room-first SQLite Cache**; local reads hit device SQLite and cost $0.00. Set a budget alert in Google Cloud Billing at $1.00 for safety. |

---

**PulsePoll is now ready for production release! 🚀**
