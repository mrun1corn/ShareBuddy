#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: $0 [APK_PATH] [ALIGNED_OUT] [SIGNED_OUT]

Environment variables (take precedence if set):
  ANDROID_SDK_ROOT or ANDROID_HOME - location of Android SDK (required)
  BUILD_TOOLS_VERSION - build-tools version (default: 33.0.2)
  RELEASE_STORE_FILE - keystore path (default: keystores/sharebuddy-test.jks)
  RELEASE_STORE_PASSWORD - keystore password (default: testpass)
  RELEASE_KEY_ALIAS - key alias (default: sharebuddy)
  RELEASE_KEY_PASSWORD - key password (default: testpass)

Examples:
  # default (uses keystores/sharebuddy-test.jks)
  $0

  # specify apk and outputs
  $0 app/build/outputs/apk/release/app-release.apk \
    app/build/outputs/apk/release/app-release-aligned.apk \
    app/build/outputs/apk/release/app-release-aligned-signed.apk
EOF
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
  usage
  exit 0
fi

BUILD_TOOLS_VERSION=${BUILD_TOOLS_VERSION:-33.0.2}
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
if [[ -z "$SDK" ]]; then
  echo "ERROR: ANDROID_SDK_ROOT or ANDROID_HOME must be set." >&2
  exit 2
fi

APK=${1:-app/build/outputs/apk/release/app-release.apk}
ALIGNED=${2:-app/build/outputs/apk/release/app-release-aligned.apk}
SIGNED=${3:-app/build/outputs/apk/release/app-release-aligned-signed.apk}

KS=${RELEASE_STORE_FILE:-keystores/sharebuddy-test.jks}
KSPASS=${RELEASE_STORE_PASSWORD:-testpass}
ALIAS=${RELEASE_KEY_ALIAS:-sharebuddy}
KPASS=${RELEASE_KEY_PASSWORD:-testpass}

BUILD_TOOLS_DIR="$SDK/build-tools/$BUILD_TOOLS_VERSION"
ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"

echo "Using SDK: $SDK"
echo "Using build-tools: $BUILD_TOOLS_VERSION"

if [[ ! -x "$ZIPALIGN" ]]; then
  echo "ERROR: zipalign not found at $ZIPALIGN" >&2
  exit 2
fi
if [[ ! -x "$APKSIGNER" ]]; then
  echo "ERROR: apksigner not found at $APKSIGNER" >&2
  exit 2
fi

if [[ ! -f "$APK" ]]; then
  echo "ERROR: input APK not found: $APK" >&2
  exit 2
fi

echo "Zipaligning: $APK -> $ALIGNED"
"$ZIPALIGN" -v -p 4 "$APK" "$ALIGNED"

echo "Signing: $ALIGNED -> $SIGNED"
"$APKSIGNER" sign \
  --ks "$KS" \
  --ks-key-alias "$ALIAS" \
  --ks-pass pass:"$KSPASS" \
  --key-pass pass:"$KPASS" \
  --out "$SIGNED" "$ALIGNED"

echo "Verifying signature for $SIGNED"
"$APKSIGNER" verify --print-certs "$SIGNED"

echo "Signed APK: $SIGNED"

exit 0
