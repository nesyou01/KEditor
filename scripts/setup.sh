#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FFMPEG_SCRIPT="$ROOT_DIR/scripts/build-ffmpeg.sh"
X264_SCRIPT="$ROOT_DIR/scripts/build-x264.sh"

ANDROID_API="${ANDROID_API:-24}"
MIN_IOS="${MIN_IOS:-15.0}"

: "${TOOLCHAIN:?TOOLCHAIN environment variable must be set}"

# ── Android arm64-v8a ───────────────────────────────────────────────────────

echo "Building x264 for Android arm64-v8a..."

(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=aarch64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$X264_SCRIPT"
)

echo "Building FFmpeg for Android arm64-v8a..."

(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=aarch64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$FFMPEG_SCRIPT"
)

# ── Android x86_64 ──────────────────────────────────────────────────────────

echo "Building x264 for Android x86_64..."

(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=x86_64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$X264_SCRIPT"
)

echo "Building FFmpeg for Android x86_64..."

(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=x86_64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$FFMPEG_SCRIPT"
)

# ── iOS arm64 ───────────────────────────────────────────────────────────────

echo "Building x264 for iOS arm64..."

(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphoneos \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$X264_SCRIPT"
)

echo "Building FFmpeg for iOS arm64..."

(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphoneos \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$FFMPEG_SCRIPT"
)

# ── iOS Simulator arm64 ─────────────────────────────────────────────────────

echo "Building x264 for iOS Simulator arm64..."

(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphonesimulator \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$X264_SCRIPT"
)

echo "Building FFmpeg for iOS Simulator arm64..."

(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphonesimulator \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$FFMPEG_SCRIPT"
)

echo
echo "Running setup-keditor.sh..."

"$ROOT_DIR/scripts/setup-keditor.sh"

echo
echo "========================================"
echo "All targets built successfully"
echo "========================================"