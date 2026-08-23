#!/usr/bin/env bash
set -euo pipefail

# ── Defaults ────────────────────────────────────────────────────────────────

PLATFORM="${PLATFORM:-android}"
ARCH="${ARCH:-aarch64}"
API_LEVEL="${API_LEVEL:-24}"
MIN_IOS="${MIN_IOS:-15.0}"

: "${TOOLCHAIN:?TOOLCHAIN env var must be set for Android}"

# ── Paths ───────────────────────────────────────────────────────────────────

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="$ROOT_DIR/native/keditor/keditor.c"

if [[ ! -f "$SOURCE" ]]; then
    echo "Source file not found: $SOURCE"
    exit 1
fi

# ── Platform configuration ─────────────────────────────────────────────────

case "$PLATFORM" in

    android)

        case "$ARCH" in
            aarch64)
                ANDROID_ABI="arm64-v8a"
                TRIPLE="aarch64-linux-android${API_LEVEL}"
                ;;
            x86_64)
                ANDROID_ABI="x86_64"
                TRIPLE="x86_64-linux-android${API_LEVEL}"
                ;;
            *)
                echo "Unsupported Android architecture: $ARCH"
                exit 1
                ;;
        esac

        CC="$TOOLCHAIN/${TRIPLE}-clang"
        AR="$TOOLCHAIN/llvm-ar"
        RANLIB="$TOOLCHAIN/llvm-ranlib"

        PREFIX="$ROOT_DIR/native/keditor/lib/android/$ANDROID_ABI"

        # FFmpeg headers
        FFMPEG_INCLUDE_DIR="$ROOT_DIR/native/ffmpeg/android/$ANDROID_ABI/include"

        CFLAGS=(
            "-fPIC"
            "-O2"
            "-DNDEBUG"
            "-I$FFMPEG_INCLUDE_DIR"
        )

        ;;

    darwin)

        : "${SDK:?SDK must be set for Darwin (iphoneos or iphonesimulator)}"

        SDK_PATH="$(xcrun --sdk "$SDK" --show-sdk-path)"
        CC="$(xcrun --sdk "$SDK" --find clang)"
        AR="$(xcrun --sdk "$SDK" --find ar)"
        RANLIB="$(xcrun --sdk "$SDK" --find ranlib)"

        case "$ARCH" in
            arm64)
                ;;
            x86_64)
                ;;
            *)
                echo "Unsupported Darwin architecture: $ARCH"
                exit 1
                ;;
        esac

        PREFIX="$ROOT_DIR/native/keditor/lib/darwin/$SDK/$ARCH"

        # FFmpeg headers
        FFMPEG_INCLUDE_DIR="$ROOT_DIR/native/ffmpeg/darwin/$SDK/$ARCH/include"

        if [[ "$SDK" == "iphoneos" ]]; then
            MIN_FLAG="-mios-version-min=${MIN_IOS}"
        else
            MIN_FLAG="-mios-simulator-version-min=${MIN_IOS}"
        fi

        CFLAGS=(
            "-arch" "$ARCH"
            "-isysroot" "$SDK_PATH"
            "$MIN_FLAG"
            "-fPIC"
            "-O2"
            "-DNDEBUG"
            "-I$FFMPEG_INCLUDE_DIR"
        )

        ;;

    *)
        echo "Unsupported PLATFORM: $PLATFORM"
        echo "Supported platforms: android, darwin"
        exit 1
        ;;

esac

# ── Validate FFmpeg headers ─────────────────────────────────────────────────

if [[ ! -d "$FFMPEG_INCLUDE_DIR" ]]; then
    echo "FFmpeg include directory not found:"
    echo "  $FFMPEG_INCLUDE_DIR"
    exit 1
fi

# ── Build directories ───────────────────────────────────────────────────────

BUILD_DIR="$ROOT_DIR/build/keditor/$PLATFORM/$ARCH"
OBJ="$BUILD_DIR/keditor.o"

rm -rf "$BUILD_DIR"

mkdir -p "$BUILD_DIR"
mkdir -p "$PREFIX/lib"

# ── Compile ─────────────────────────────────────────────────────────────────

echo "Compiling:"
echo "  Source  : $SOURCE"
echo "  CC      : $CC"
echo "  Arch    : $ARCH"
echo "  FFmpeg  : $FFMPEG_INCLUDE_DIR"
echo "  Output  : $PREFIX"

"$CC" \
    "${CFLAGS[@]}" \
    -c "$SOURCE" \
    -o "$OBJ"

# ── Create static library ───────────────────────────────────────────────────

"$AR" rcs \
    "$PREFIX/lib/libkeditor.a" \
    "$OBJ"

"$RANLIB" \
    "$PREFIX/lib/libkeditor.a" \
    2>/dev/null || true

# ── Done ────────────────────────────────────────────────────────────────────

echo
echo "========================================"
echo "keditor.c build completed"
echo "Platform : $PLATFORM"
echo "Arch     : $ARCH"
echo "Output   : $PREFIX"
echo "Library  : $PREFIX/lib/libkeditor.a"
echo "========================================"