# Share Buddy

Share Buddy is an Android share target built with Jetpack Compose and Material 3. It catches anything you share—links, snippets of text, or images—and drops it into a polished inbox where you can search, filter, pin, remind, and re-share without losing context.

## Highlights
- **Share from anywhere**: the Share Buddy sheet offers Save, Clean + Re-share, Remind, and Re-share shortcuts the moment you invoke Android’s share menu.
- **Material You inbox**: unified top bar with search, refresh, sort, and live counts, plus a bottom navigation bar for quick filters (All, Links, Text, Images).
- **Cleaner links & reminders**: remove tracking junk before resharing, and schedule notifications with optional auto-cleanup once the reminder fires.
- **Link-friendly cards**: saved links are tappable, plain text is fully selectable, and pinned items rise to the top.
- **Offline-first**: data lives in a Room database on-device—no accounts or cloud sync required.

## Usage
1. **Install the app** (see Installation below).
2. From any app, choose **Share → Share Buddy**. The inline sheet shows a preview of the content plus quick actions:
   - **Save** stores text, link, or image (with an optional label).
   - **Clean + Re-share** sanitizes URLs and hands them back to the system share sheet without saving.
   - **Remind** prompts you to schedule a notification so the item resurfaces later.
   - **Re-share** forwards the original content as-is.
3. Open the Share Buddy app to manage your inbox:
   - Top bar: tap the search icon to reveal inline search, use refresh to reset filters, and sort by date/name/label.
   - Bottom bar: switch between All, Links, Text, and Images.
   - Item cards: tap the title of link items to open them, copy contents via the “copy” icon, pin for quick access, edit labels, or delete. Reminders show as chips and can be managed from each card.

## Installation
### Option 1: Download release APK
Grab the latest signed build from the [GitHub Releases page](https://github.com/mrun1corn/ShareBuddy/releases). Install the `app-release.apk` on your device (enable “Install from unknown sources” if prompted).

### Option 2: Build from source
1. Install prerequisites (Ubuntu example):
   ```bash
   sudo apt-get update
   sudo apt-get install -y openjdk-17-jdk unzip wget

   # Gradle via SDKMAN
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   sdk install gradle 8.8

   # Android command-line SDK
   mkdir -p $HOME/Android/cmdline-tools
   cd $HOME/Android
   wget -O commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
   unzip commandlinetools.zip -d cmdline-tools
   mv cmdline-tools/cmdline-tools cmdline-tools/latest

   export ANDROID_SDK_ROOT="$HOME/Android"
   export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

   yes | sdkmanager --licenses
   sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
   ```
2. Clone the repo and build:
   ```bash
   git clone https://github.com/mrun1corn/ShareBuddy.git
   cd ShareBuddy
   ./gradlew assembleDebug
   ```
   The debug APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

## Signing & Release Builds
1. Generate a keystore (run once):
   ```bash
   keytool -genkeypair \
     -v \
     -keystore $HOME/keystores/sharebuddy-release.jks \
     -alias sharebuddy \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000
   ```
2. Add credentials to `~/.gradle/gradle.properties` (preferred) or pass them via CI secrets:
   ```
   RELEASE_STORE_FILE=/home/robin/keystores/sharebuddy-release.jks
   RELEASE_STORE_PASSWORD=YourStorePassword
   RELEASE_KEY_ALIAS=sharebuddy
   RELEASE_KEY_PASSWORD=YourKeyPassword
   ```
3. Build the signed artifact:
   ```bash
   ./gradlew assembleRelease
   ```
   The APK will be placed at `app/build/outputs/apk/release/app-release.apk`.

> Note: For convenience, the project falls back to the debug keystore if no release keystore is configured. Only ship the release keystore build to stores.

## Tech Stack
- Kotlin 2.x, Jetpack Compose, and Material 3 components
- Room for persistence, Coil for image loading, Jsoup for link cleaning
- Reminder scheduling via `AlarmManager` with notification permission handling on Android 13+

## Contributing
Issues and pull requests are welcome! Please make sure code builds with `./gradlew assembleDebug` and includes updates to documentation/tests when appropriate.
