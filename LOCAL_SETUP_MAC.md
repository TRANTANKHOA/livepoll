# 🍏 macOS Local Development Guide - PulsePoll

This guide provides step-by-step instructions and automated scripts to set up, build, test, and run **PulsePoll** locally on **macOS** (both **Apple Silicon M1/M2/M3/M4** and **Intel x86_64**).

---

## ⚡ Quick Start (Automated 1-Command Setup)

Open Terminal in the project root directory and run the automated setup script:

```bash
chmod +x scripts/*.sh gradlew
./scripts/setup-mac.sh
```

This script will automatically:
1. Detect your Mac architecture (`arm64` Apple Silicon or `x86_64` Intel).
2. Check and install **Homebrew** if missing.
3. Install and link **OpenJDK 21**.
4. Configure your Android SDK environment variables (`ANDROID_HOME`).
5. Ensure `adb` (Android Debug Bridge) is available in your `$PATH`.
6. Make all developer helper scripts executable.

---

## 🛠️ Prerequisites & Manual Setup

If you prefer setting up manually or need specific configurations:

### 1. Install Homebrew (if not already installed)
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. Install Java (JDK 21 or JDK 17)
```bash
brew install openjdk@21

# Symlink to macOS system Java VM path
sudo ln -sfn $(brew --prefix openjdk@21)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

Add Java to your shell profile (`~/.zshrc` or `~/.zprofile`):
```bash
# For Apple Silicon (M1/M2/M3/M4)
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc

# For Intel Macs
# echo 'export PATH="/usr/local/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
# echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc

source ~/.zshrc
```

Verify Java:
```bash
java -version
```

### 3. Install Android Studio & SDK Tools
Install Android Studio using Homebrew:
```bash
brew install --cask android-studio
```
Or download directly from [developer.android.com/studio](https://developer.android.com/studio).

### 4. Configure Android Environment Variables
Add Android SDK tools to your `~/.zshrc`:
```bash
cat << 'EOF' >> ~/.zshrc

# Android SDK Configuration
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export PATH="$ANDROID_HOME/emulator:$PATH"
EOF

source ~/.zshrc
```

---

## 💻 Working with Android Studio

1. **Launch Android Studio** on macOS:
   ```bash
   open -a "Android Studio" .
   ```
2. In the setup wizard or welcome dialog, select **"Open"** and choose this project root folder.
3. Allow Gradle to perform the initial **Sync** (it will resolve all dependencies and Kotlin Symbol Processing / KSP modules).
4. **Set up an Android Emulator (AVD)**:
   - Navigate to **Tools** > **Device Manager**.
   - Click **Create Device** (e.g., *Pixel 8* or *Pixel 9*).
   - Select **System Image**: Choose **API 34 or API 35 (Android 14/15)** with the **`arm64-v8a`** ABI (for Apple Silicon) or `x86_64` (for Intel).
   - Click **Finish** and start the emulator by clicking ▶️.
5. **Run the App**:
   - Press `Control + R` (or click the green ▶️ **Run** button in the top toolbar).

---

## 🚀 Terminal Developer Scripts

The repository includes convenient shell scripts in `/scripts/` tailored for macOS workflow:

### 🧪 1. Run Unit & Robolectric Tests
Executes the full test suite including consensus math, Room SQLite CRUD, and ViewModel flows:
```bash
./scripts/run-tests.sh
```
*To view the HTML report in Safari/Chrome:*
```bash
open app/build/reports/tests/testDebugUnitTest/index.html
```

### 📦 2. Build Debug APK
Compiles the application and generates the `.apk` file:
```bash
./scripts/build-apk.sh
```
*To reveal the APK in macOS Finder:*
```bash
open -R app/build/outputs/apk/debug/app-debug.apk
```

### 📱 3. Install & Launch on Device/Emulator
Builds the latest debug binary, pushes it via `adb`, and starts the main activity:
```bash
./scripts/run-app.sh
```

### 🧹 4. Clean & Rebuild
Cleans cached outputs and rebuilds from scratch:
```bash
./scripts/clean-build.sh
```

---

## 🔍 Useful macOS CLI Commands & Tips

| Task | Command |
| :--- | :--- |
| **Stream Logcat Logs** | `adb logcat -s PulsePoll:* -v color` |
| **List Connected Devices** | `adb devices` |
| **Install APK Directly via ADB** | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| **Take Screenshot from Emulator** | `adb exec-out screencap -p > screenshot.png` |
| **Record Screen Video** | `adb shell screenrecord /sdcard/demo.mp4 && adb pull /sdcard/demo.mp4 .` |
| **Record Roborazzi Screenshot Tests** | `./gradlew :app:recordRoborazziDebug` |
| **Verify Roborazzi Screenshot Tests** | `./gradlew :app:verifyRoborazziDebug` |

---

## 🔧 Troubleshooting on macOS

### 1. `adb: command not found`
Ensure your `~/.zshrc` has `export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"`.
Run `source ~/.zshrc`.

### 2. Gradle JVM Compatibility Warning
In Android Studio:
- Go to **Settings (⌘ + ,)** > **Build, Execution, Deployment** > **Build Tools** > **Gradle**.
- Set **Gradle JDK** to **Embedded JDK 21** or your Homebrew OpenJDK 21.

### 3. macOS Security / Gatekeeper Warnings on Tools
If macOS blocks execution of downloaded SDK binaries:
```bash
xattr -dr com.apple.quarantine $HOME/Library/Android/sdk
```

---

*Happy coding on macOS! ⚡ PulsePoll Team*
