#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIGURE_SCRIPT="$ROOT_DIR/scripts/ffmpeg_configure.sh"

ANDROID_API="${ANDROID_API:-24}"
MIN_IOS="${MIN_IOS:-15.0}"

: "${TOOLCHAIN:?TOOLCHAIN environment variable must be set}"

# ── Android arm64-v8a ───────────────────────────────────────────────────────

echo "Configuring Android arm64-v8a..."

(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=aarch64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$CONFIGURE_SCRIPT"
)

# ── Android x86_64 ──────────────────────────────────────────────────────────

echo "Configuring Android x86_64..."

(
    cd "$ROOT_DIR"

    PLATFORM=android \
    ARCH=x86_64 \
    API_LEVEL="$ANDROID_API" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$CONFIGURE_SCRIPT"
)

# ── iOS arm64 ───────────────────────────────────────────────────────────────

echo "Configuring iOS arm64..."

(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphoneos \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$CONFIGURE_SCRIPT"
)

# ── iOS Simulator arm64 ─────────────────────────────────────────────────────

echo "Configuring iOS Simulator arm64..."

(
    cd "$ROOT_DIR"

    PLATFORM=darwin \
    ARCH=arm64 \
    SDK=iphonesimulator \
    MIN_IOS="$MIN_IOS" \
    TOOLCHAIN="$TOOLCHAIN" \
    "$CONFIGURE_SCRIPT"
)

echo
echo "========================================"
echo "All FFmpeg targets configured"
echo "========================================"