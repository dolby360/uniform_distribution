#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
ADB="$HOME/Android/Sdk/platform-tools/adb.exe"
EMULATOR="$HOME/Android/Sdk/emulator/emulator.exe"
AVD_NAME="uniform_dist_test"
PACKAGE="com.uniformdist.app"
ACTIVITY="$PACKAGE/.MainActivity"

# Check if a device/emulator is connected
if ! "$ADB" devices 2>/dev/null | grep -q 'device$'; then
  echo "==> No device found. Starting emulator ($AVD_NAME)..."
  "$EMULATOR" -avd "$AVD_NAME" &
  echo "==> Waiting for emulator to boot..."
  "$ADB" wait-for-device
  # Wait until the device has fully booted
  while [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
    sleep 2
  done
  echo "==> Emulator ready."
fi

echo "==> Building debug APK..."
cd "$ANDROID_DIR"
./gradlew assembleDebug

echo "==> Installing on device..."
./gradlew installDebug

echo "==> Launching app..."
"$ADB" shell am force-stop "$PACKAGE"
"$ADB" shell am start -n "$ACTIVITY"

echo "==> Done! App reloaded."
