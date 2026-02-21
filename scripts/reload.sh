#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
ADB="$HOME/Android/Sdk/platform-tools/adb.exe"
PACKAGE="com.uniformdist.app"
ACTIVITY="$PACKAGE/.MainActivity"

echo "==> Building debug APK..."
cd "$ANDROID_DIR"
./gradlew assembleDebug

echo "==> Installing on device..."
./gradlew installDebug

echo "==> Launching app..."
"$ADB" shell am force-stop "$PACKAGE"
"$ADB" shell am start -n "$ACTIVITY"

echo "==> Done! App reloaded."
