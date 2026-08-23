#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

KEDITOR_C_SCRIPT="$ROOT_DIR/scripts/build-keditor.sh"

ANDROID_API="${ANDROID_API:-24}"
MIN_IOS="${MIN_IOS:-15.0}"

: "${TOOLCHAIN:?TOOLCHAIN environment variable must be set}"


echo "Building keditor for Android arm64-v8a..."
(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=aarch64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$KEDITOR_C_SCRIPT"
)


echo "Building keditor for Android x86_64..."
(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=x86_64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$KEDITOR_C_SCRIPT"
)

echo "Building keditor for iOS arm64..."
(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphoneos \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$KEDITOR_C_SCRIPT"
)

echo "Building keditor for iOS Simulator arm64..."
(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphonesimulator \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$KEDITOR_C_SCRIPT"
)

