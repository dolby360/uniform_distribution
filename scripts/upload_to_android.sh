#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
ADB="$HOME/Android/Sdk/platform-tools/adb.exe"
PACKAGE="com.uniformdist.app"
ACTIVITY="$PACKAGE/.MainActivity"
ONEPLUS_SERIAL="b7994932"

# Verify device is connected
if ! "$ADB" devices | grep -q "$ONEPLUS_SERIAL"; then
  echo "ERROR: OnePlus 12 not found. Make sure it's connected with USB debugging enabled."
  exit 1
fi

echo "==> Building debug APK..."
cd "$ANDROID_DIR"
./gradlew assembleDebug

echo "==> Installing on OnePlus 12..."
"$ADB" -s "$ONEPLUS_SERIAL" install -r "$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "==> Launching app..."
"$ADB" -s "$ONEPLUS_SERIAL" shell am force-stop "$PACKAGE"
"$ADB" -s "$ONEPLUS_SERIAL" shell am start -n "$ACTIVITY"

echo "==> Done! App installed on OnePlus 12."
