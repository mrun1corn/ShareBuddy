# Share Buddy — com.mrunicorn.sb (Material 3, CLI build)

A **smart Share Target**: when you share text/links/images from any app, Share Buddy gives you **Save**, **Clean (links)**, **Remind**, **Re‑share** — and a friendly **Material You** inbox (Compose Material 3).

No Android Studio needed; build from Ubuntu CLI.

## Setup (Ubuntu)
```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk unzip wget

# Gradle via SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.8

# Android SDK (commandline)
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

## Build

To build the application, navigate to the project root directory and run the following command:

```bash
./gradlew assembleDebug
```

The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Features
- Share Target for text/links/images
- **Save** to inbox; **Pin**; **Search**; **Filters** (All / Links / Text / Images)
- **Clean** URLs (remove trackers) and **Re‑share**
- **Remind** (AlarmManager + notifications; runtime permission on Android 13+)
- **Copy back** to clipboard
- Local-only storage (Room). No sync in this MVP.
- Material 3 **dynamic color** support on Android 12+
