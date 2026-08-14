#!/usr/bin/env bash

set -euo pipefail

# ── Defaults ────────────────────────────────────────────────────────────────

PLATFORM="${PLATFORM:-android}"
ARCH="${ARCH:-aarch64}"
API_LEVEL="${API_LEVEL:-24}"
MIN_IOS="${MIN_IOS:-15.0}"

JOBS="${JOBS:-$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)}"

: "${TOOLCHAIN:?TOOLCHAIN env var must be set}"

# ── Platform configuration ─────────────────────────────────────────────────

case "$PLATFORM" in

    android)

        TARGET_OS="android"

        TRIPLE="${ARCH}-linux-android${API_LEVEL}"

        CC="$TOOLCHAIN/${TRIPLE}-clang"
        CXX="$TOOLCHAIN/${TRIPLE}-clang++"

        AR="$TOOLCHAIN/llvm-ar"
        RANLIB="$TOOLCHAIN/llvm-ranlib"
        STRIP="$TOOLCHAIN/llvm-strip"

        case "$ARCH" in
            aarch64)
                ANDROID_ABI="arm64-v8a"
                ;;
            x86_64)
                ANDROID_ABI="x86_64"
                ;;
        esac

        PREFIX="$PWD/native/ffmpeg/android/$ANDROID_ABI"

        CONFIGURE_EXTRA=(
            --enable-jni
            --enable-mediacodec
            --enable-neon
        )

        ;;

    darwin)

        TARGET_OS="darwin"

        : "${SDK:?SDK must be set for Darwin (iphoneos or iphonesimulator)}"

        SDK_PATH="$(xcrun --sdk "$SDK" --show-sdk-path)"

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

        CC="$(xcrun --sdk "$SDK" --find clang)"
        CXX="$(xcrun --sdk "$SDK" --find clang++)"

        AR="$(xcrun --sdk "$SDK" --find ar)"
        RANLIB="$(xcrun --sdk "$SDK" --find ranlib)"
        STRIP="$(xcrun --sdk "$SDK" --find strip)"

        PREFIX="$PWD/native/ffmpeg/darwin/${SDK}/${ARCH}"

        if [[ "$SDK" == "iphoneos" ]]; then
            MIN_FLAG="-mios-version-min=${MIN_IOS}"
        else
            MIN_FLAG="-mios-simulator-version-min=${MIN_IOS}"
        fi

        CFLAGS=(
            "-arch"
            "$ARCH"
            "-isysroot"
            "$SDK_PATH"
            "$MIN_FLAG"
        )

        LDFLAGS=(
            "-arch"
            "$ARCH"
            "-isysroot"
            "$SDK_PATH"
            "$MIN_FLAG"
        )

        CONFIGURE_EXTRA=(
            --enable-neon
        )

        ;;

    *)
        echo "Unsupported PLATFORM: $PLATFORM"
        echo "Supported platforms: android, darwin"
        exit 1
        ;;

esac

# ── Clean previous build ────────────────────────────────────────────────────

rm -rf "$PREFIX"

# ── Configure ────────────────────────────────────────────────────────────────

CONFIGURE_ARGS=(
    --target-os="$TARGET_OS"
    --arch="$ARCH"
    --enable-cross-compile

    --cc="$CC"
    --cxx="$CXX"
    --ar="$AR"
    --ranlib="$RANLIB"
    --strip="$STRIP"

    --disable-programs
    --disable-doc
    --disable-debug

    --disable-shared
    --enable-static
    --enable-pic

    --prefix="$PREFIX"
)

# Darwin-specific flags
if [[ "$PLATFORM" == "darwin" ]]; then

    CONFIGURE_ARGS+=(
        --sysroot="$SDK_PATH"
        --extra-cflags="${CFLAGS[*]}"
        --extra-ldflags="${LDFLAGS[*]}"
    )

fi

# Add platform-specific options
CONFIGURE_ARGS+=("${CONFIGURE_EXTRA[@]}")

cd third-party/ffmpeg

./configure "${CONFIGURE_ARGS[@]}"

# ── Build ────────────────────────────────────────────────────────────────────

make -j"$JOBS"

make install

rm -rf "$PREFIX"/share "$PREFIX"/lib/pkgconfig

echo
echo "========================================"
echo "FFmpeg build completed"
echo "Platform : $PLATFORM"
echo "Arch     : $ARCH"
echo "Output   : $PREFIX"
echo "========================================"