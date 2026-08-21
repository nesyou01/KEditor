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
            *)
                echo "Unsupported Android architecture: $ARCH"
                exit 1
                ;;
        esac

        X264_PREFIX="$PWD/native/x264/android/$ANDROID_ABI"
        PREFIX="$PWD/native/ffmpeg/android/$ANDROID_ABI"

        # Force PIC for every compilation unit
        export CFLAGS="-fPIC -fvisibility=hidden"
        export CXXFLAGS="-fPIC -fvisibility=hidden"

        CONFIGURE_EXTRA=(
            --enable-jni
            --enable-mediacodec
            --enable-neon
            --enable-gpl
            --enable-libx264

            --enable-pic

            --extra-cflags="-fPIC -fvisibility=hidden -I$X264_PREFIX/include"
            --extra-cxxflags="-fPIC -fvisibility=hidden"

            # x264 library
            --extra-ldflags="-L$X264_PREFIX/lib"
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

        X264_PREFIX="$PWD/native/x264/darwin/${SDK}/${ARCH}"
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

            "-fPIC"

            "-I$X264_PREFIX/include"
        )

        LDFLAGS=(
            "-arch"
            "$ARCH"

            "-isysroot"
            "$SDK_PATH"

            "$MIN_FLAG"

            "-L$X264_PREFIX/lib"
        )

        # Force PIC
        export CFLAGS="${CFLAGS[*]}"
        export CXXFLAGS="${CFLAGS[*]}"

        CONFIGURE_EXTRA=(
            --enable-neon

            --enable-gpl
            --enable-libx264

            --enable-pic

            --extra-cflags="${CFLAGS[*]}"
            --extra-cxxflags="${CFLAGS[*]}"
            --extra-ldflags="${LDFLAGS[*]}"
        )

        ;;

    *)

        echo "Unsupported PLATFORM: $PLATFORM"
        echo "Supported platforms: android, darwin"
        exit 1

        ;;

esac

# ── Verify x264 ─────────────────────────────────────────────────────────────

if [[ ! -f "$X264_PREFIX/lib/libx264.a" ]]; then
    echo
    echo "ERROR: x264 was not found:"
    echo
    echo "  $X264_PREFIX/lib/libx264.a"
    echo
    echo "Run build-x264.sh first."
    exit 1
fi

# ── pkg-config ──────────────────────────────────────────────────────────────

export PKG_CONFIG_PATH="$X264_PREFIX/lib/pkgconfig${PKG_CONFIG_PATH:+:$PKG_CONFIG_PATH}"
export PKG_CONFIG_LIBDIR="$X264_PREFIX/lib/pkgconfig"

# ── Clean previous installation ─────────────────────────────────────────────

echo
echo "Cleaning previous FFmpeg installation..."

rm -rf "$PREFIX"

# ── FFmpeg source ──────────────────────────────────────────────────────────

cd third-party/ffmpeg

echo
echo "Cleaning FFmpeg build..."

make distclean >/dev/null 2>&1 || true


# ── Configure ───────────────────────────────────────────────────────────────

CONFIGURE_ARGS=(
    --target-os="$TARGET_OS"
    --arch="$ARCH"

    --enable-cross-compile

    --cc="$CC"
    --cxx="$CXX"
    --ar="$AR"
    --ranlib="$RANLIB"
    --strip="$STRIP"

    # Static FFmpeg libraries
    --disable-shared
    --enable-static

    # PIC is required because the .a files are linked
    # into the final Android/iOS shared library.
    --enable-pic

    # Reduce unnecessary FFmpeg components
    --disable-programs
    --disable-doc
    --disable-debug

    --prefix="$PREFIX"
)

# ── Darwin configuration ───────────────────────────────────────────────────

if [[ "$PLATFORM" == "darwin" ]]; then

    CONFIGURE_ARGS+=(
        --sysroot="$SDK_PATH"
        --extra-cflags="${CFLAGS[*]}"
        --extra-cxxflags="${CXXFLAGS}"
        --extra-ldflags="${LDFLAGS[*]}"
    )

fi

# ── Platform-specific options ───────────────────────────────────────────────

CONFIGURE_ARGS+=("${CONFIGURE_EXTRA[@]}")

echo
echo "========================================"
echo "FFmpeg configuration"
echo "========================================"
echo "Platform : $PLATFORM"
echo "Arch     : $ARCH"
echo "Compiler : $CC"
echo "x264     : $X264_PREFIX"
echo "Output   : $PREFIX"
echo "Jobs     : $JOBS"
echo "CFLAGS   : $CFLAGS"
echo "========================================"
echo

./configure "${CONFIGURE_ARGS[@]}"

# ── Show generated configuration ────────────────────────────────────────────

echo
echo "FFmpeg configured with:"
echo

grep -E 'CONFIG_(PIC|ASM|X264)' config.h 2>/dev/null || true

echo
echo "Building FFmpeg..."
echo

# V=1 makes it possible to verify that -fPIC is actually passed
# to tx_float.o and other FFmpeg compilation units.
make V=1 -j"$JOBS"

# ── Install ─────────────────────────────────────────────────────────────────

echo
echo "Installing FFmpeg..."

make install

# Remove unnecessary files
rm -rf "$PREFIX/share"
rm -rf "$PREFIX/lib/pkgconfig"

# ── Done ────────────────────────────────────────────────────────────────────

echo
echo "========================================"
echo "FFmpeg build completed successfully"
echo "========================================"
echo "Platform : $PLATFORM"
echo "Arch     : $ARCH"
echo "Output   : $PREFIX"
echo
echo "Libraries:"
find "$PREFIX/lib" -name "*.a" -maxdepth 1 -print 2>/dev/null || true
echo "========================================"